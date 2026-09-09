# Smart Todo enrich function

HTTP-triggered Azure Function (Node.js 24, Functions v4 programming model)
that sits between the Smart Todo Android app and a large language model. The
app never holds a model API key; it calls this function with a function key,
and the function holds the model credentials.

The model is reached over an **OpenAI-compatible chat completions API**, using
the official `openai` npm client. The provider is DeepInfra by default, but
nothing in the code is DeepInfra-specific: the endpoint, the credential and
the model id are all App Settings.

## API contract

This contract is frozen. The Android client is written against it exactly. It
did not change when the provider changed.

```
POST /api/enrich
Content-Type: application/json
x-functions-key: <function key>

{ "text": "kal client ko presentation bhejni hai" }
```

Success, HTTP 200:

```json
{
  "englishText": "Send the presentation to the client tomorrow",
  "category": "WORK",
  "confidence": 0.92
}
```

Errors are always JSON of the shape `{ "error": "<code>", "message": "<text>" }`:

| Status | `error`          | Cause |
| ------ | ---------------- | ----- |
| 400    | `invalid_input`  | body is not JSON, `text` missing / not a string / empty, or over 500 characters |
| 429    | `rate_limited`   | caller exceeded the per-IP or global limit; a `Retry-After` header is set |
| 502    | `upstream_error` | the model call failed, its output was unusable, or the function is not configured |

`category` is always one of: `WORK`, `PERSONAL`, `HEALTH_FITNESS`, `CODING`,
`STUDY`, `SHOPPING`, `FINANCE`, `OTHER`. Any other value produced by the model
is mapped to `OTHER`, and its reported confidence is capped at `0.5`.

## How it works

- Transport: `POST {LLM_BASE_URL}/chat/completions` via the `openai` client,
  15 second timeout, one retry. Default model `anthropic/claude-haiku-4-5`,
  `max_tokens` 256. This is a short translate-and-classify task with a tiny
  structured output, so the cheapest capable model is the right trade-off.
- Structured output is forced with an OpenAI function tool: a single function
  (`record_task`) whose parameters require `englishText`, `category` (an
  `enum` over the closed category set) and `confidence`, combined with
  `tool_choice: { type: 'function', function: { name: 'record_task' } }`.
  `strict: true` is set on the function so endpoints that support constrained
  decoding enforce the schema, but nothing depends on it.
- The tool call's arguments arrive as a **JSON string**, not an object, and
  are `JSON.parse`d. A parse failure is a 502, never a guess.
- Fallback for models that ignore `tool_choice`: if the response carries no
  `tool_calls` but the assistant content parses as a JSON object, that object
  is used. It is then held to exactly the same validation and category
  coercion as a real tool call. Content that does not parse is a 502.
- The user's text is sent as the entire user message and is never interpolated
  into the system prompt, so there is no delimiter for hostile input to escape
  from. The system prompt states that the user message is untrusted data.
  Even if the model is talked into misbehaving, the category is validated
  server-side against the closed set.
- Input hardening: control characters (C0, DEL, C1) become spaces, whitespace
  runs are collapsed, and text over 500 characters is rejected. Payloads over
  2000 raw characters are rejected before sanitising.

## Layout

```
src/functions/enrich.js   HTTP trigger: rate limit, validate, call, respond
src/lib/prompt.js         system prompt and tool schema          (review this)
src/lib/llm.js            provider settings, client, model call, parsing
src/lib/categories.js     frozen category set and coercion rules
src/lib/validate.js       request validation and text sanitising
src/lib/rateLimit.js      in-memory sliding-window limiter
scripts/lint.mjs          dependency-free syntax check
test/                     node --test unit tests, no network calls
```

The three provider App Settings live together in `src/lib/llm.js` rather than
being split across modules, so there is one place to look when repointing the
function at a different host.

## Required App Settings

Set these on the Function App (Configuration, Application settings). Only
`LLM_API_KEY` is a secret.

| Setting | Required | Value |
| ------- | -------- | ----- |
| `LLM_API_KEY` | yes | API key for the OpenAI-compatible endpoint. A DeepInfra key today. Never commit it. Prefer a Key Vault reference. |
| `LLM_BASE_URL` | no | OpenAI-compatible base URL. Defaults to `https://api.deepinfra.com/v1/openai`. Set it explicitly in production so a provider change is visible in configuration rather than in code. |
| `LLM_MODEL` | no | Model id. Defaults to `anthropic/claude-haiku-4-5`. |
| `FUNCTIONS_WORKER_RUNTIME` | yes | `node` |
| `FUNCTIONS_EXTENSION_VERSION` | yes | `~4` |
| `WEBSITE_NODE_DEFAULT_VERSION` | no | Not used on Flex Consumption. The Node version lives in the app's `functionAppConfig.runtime` and is set at create time (`--runtime node --runtime-version 24`). |
| `AzureWebJobsStorage` | yes | storage account connection string |
| `ENRICH_RATE_LIMIT_PER_MINUTE` | no | per-IP limit, default `30` |
| `ENRICH_GLOBAL_RATE_LIMIT_PER_MINUTE` | no | limit across all callers, default `300` |
| `AzureWebJobsFeatureFlags` | only on old hosts | `EnableWorkerIndexing`. Needed only if the Functions host predates 4.34; without it an older host will not discover v4-model functions. |

The setting names are provider-neutral on purpose. Moving to any other
OpenAI-compatible host (Together, Fireworks, Groq, OpenAI itself, a local
vLLM) is a change to `LLM_BASE_URL`, `LLM_API_KEY` and `LLM_MODEL` only.

If `LLM_API_KEY` is absent, the function logs `FATAL CONFIG: ...` at startup
and answers every request with 502 `upstream_error`. The key is never logged
and never appears in a response body.

## Switching models

Change `LLM_MODEL` and restart the Function App. No code change, no redeploy.
`LLM_BASE_URL` and `LLM_MODEL` are read per request, so a restart is only
needed because the client (and therefore the base URL) is cached per worker.

Models verified as present in the DeepInfra catalogue and suitable for this
workload:

| `LLM_MODEL` | Input / output USD per MTok | Context | Notes |
| ----------- | --------------------------- | ------- | ----- |
| `anthropic/claude-haiku-4-5` | 1.00 / 5.00 | 200K | Default. Same price as Anthropic first-party. Best translation quality of the three on Hinglish and Urdulish. |
| `deepseek-ai/DeepSeek-V4-Flash` | 0.09 / 0.18 | 1M | Roughly 13x cheaper. The obvious candidate if quality holds up on real input. |
| `Qwen/Qwen3.5-397B-A17B` | 0.45 / 3.00 | 262K | Middle option. |

Before switching, check the candidate honours forced `tool_choice`. If it does
not, the content-JSON fallback described above catches it, but that path is
less reliable than a real tool call: watch the `upstream_error` rate after a
change.

## Running locally

Requires Node.js 24 or newer and Azure Functions Core Tools v4.

```
cd azure-function
npm ci
cp local.settings.json.example local.settings.json
npm start
```

Paste a real DeepInfra key into the copied `local.settings.json`. That file is
gitignored and must never be committed.

Smoke test against the local host (no function key is required locally):

```
curl -s -X POST http://localhost:7071/api/enrich -H 'Content-Type: application/json' -d '{"text":"kal gym jana hai"}'
```

Tests and lint need no network and no API key:

```
npm run lint
npm test
```

Every model call in the test suite goes through a stub client, so the suite
cannot reach a provider and cannot incur spend.

## Deploying

The deploy job in `.github/workflows/azure-function.yml` is commented out
until the publish profile secret exists. Until then, deploy from a machine
with Core Tools and a completed `az login`:

```
cd azure-function
npm ci --omit=dev
func azure functionapp publish <function-app-name>
```

Afterwards, confirm the App Settings above are set and fetch the function key
(Function App, Functions, enrich, Function Keys) for the app to embed.

To enable CI deploys: download the publish profile from the Function App,
store it as the repository secret `AZURE_FUNCTIONAPP_PUBLISH_PROFILE`, then
uncomment the deploy job and set the app name.

### Dependency note

`openai` is pinned to `7.13.0`. Version 7 requires Node 22 or newer, which the
Function App satisfies: it runs Node 24, because Azure now refuses to create a
Node 20 Function App at all (Node 20 reached end of life on 2026-04-30). The
client has no runtime dependencies of its own, so the deployed tree is
`@azure/functions` plus `openai` and nothing else.

## Rate limiting: known limitation

The function key ships inside a distributed APK, so it is only semi-secret.
The limiter defends against casual abuse: 30 requests per minute per client IP
(read from `x-forwarded-for`) and 300 per minute in aggregate, over a sliding
window.

The state is a `Map` in worker memory. It therefore:

- resets on cold start,
- is not shared across scaled-out instances, so the real ceiling is the
  configured limit multiplied by the number of live instances,
- keys off a caller-controlled header, so an attacker rotating
  `x-forwarded-for` values defeats the per-IP limit. The global cap and the
  5000-key tracking cap are the backstop.

The production answer is a durable shared limiter: Redis or an Azure Table
with atomic counters, or Azure API Management in front of the function. A
Function App spend cap and a DeepInfra spend alert are the hard ceiling.

## Cost

Billing is DeepInfra's, per token, at the rates in the switching table above.
Routing Claude through DeepInfra costs the same as Anthropic first-party
(1.00 USD per million input tokens, 5.00 USD per million output tokens) while
drawing on credit that already exists.

Measured per request: the system prompt plus tool schema is about 660 tokens
and a typical task adds 20 to 120, so roughly 750 input tokens. The forced
tool call is about 40 to 60 output tokens; call it 50.

```
anthropic/claude-haiku-4-5
  input   750 x 1.00 / 1M = 0.00075 USD
  output   50 x 5.00 / 1M = 0.00025 USD
  total                     about 0.001 USD per request

deepseek-ai/DeepSeek-V4-Flash
  input   750 x 0.09 / 1M = 0.000068 USD
  output   50 x 0.18 / 1M = 0.000009 USD
  total                     about 0.00008 USD per request

Qwen/Qwen3.5-397B-A17B
  input   750 x 0.45 / 1M = 0.00034 USD
  output   50 x 3.00 / 1M = 0.00015 USD
  total                     about 0.0005 USD per request
```

So Haiku is roughly 1000 enrichments per USD and a user creating 20 tasks a
day costs about 0.60 USD a month. DeepSeek-V4-Flash is about 13000
enrichments per USD, roughly 0.05 USD a month for the same user. That gap is
the reason `LLM_MODEL` is a setting: quality per rupee can be measured on
real traffic without shipping code.

Prompt caching is not used. The cacheable prefix here is below the minimum
cacheable prefix length, so it would have no effect.

Consumption-plan hosting adds effectively nothing at this volume beyond the
storage account. The real cost risk is not the per-request price, it is
someone replaying the embedded function key, which is what the limiter and a
spend cap exist for.

## Privacy

The task text is user personal data. It is sent to the configured model
endpoint (DeepInfra by default, which routes Anthropic models to Anthropic)
and is never written to logs. Each invocation logs one JSON line containing
only the outcome, HTTP status, duration, input length, resulting category and
token counts.
