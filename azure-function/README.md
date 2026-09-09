# Smart Todo enrich function

HTTP-triggered Azure Function (Node.js 20, Functions v4 programming model)
that sits between the Smart Todo Android app and the Claude API. The app never
holds an Anthropic API key; it calls this function with a function key, and the
function holds the model credentials.

## API contract

This contract is frozen. The Android client is written against it exactly.

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

- Model: `claude-haiku-4-5-20251001`, `max_tokens` 256. This is a short
  translate-and-classify task with a tiny structured output, so the cheapest
  and fastest model in the family is the right trade-off.
- Structured output is forced with Anthropic tool use: a single tool
  (`record_task`) whose schema requires `englishText`, `category` (an `enum`
  over the closed category set) and `confidence`, combined with
  `tool_choice: { type: 'tool', name: 'record_task' }`. Free-form prose is
  never parsed. If the tool call is missing or unusable the function returns
  502 rather than guessing.
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
src/lib/prompt.js         model id, system prompt, tool schema  (review this)
src/lib/claude.js         Anthropic client, model call, response parsing
src/lib/categories.js     frozen category set and coercion rules
src/lib/validate.js       request validation and text sanitising
src/lib/rateLimit.js      in-memory sliding-window limiter
scripts/lint.mjs          dependency-free syntax check
test/                     node --test unit tests, no network calls
```

## Required App Settings

Set these on the Function App (Configuration, Application settings). Only the
first is a secret.

| Setting | Required | Value |
| ------- | -------- | ----- |
| `ANTHROPIC_API_KEY` | yes | Anthropic API key. Never commit it. Prefer a Key Vault reference. |
| `FUNCTIONS_WORKER_RUNTIME` | yes | `node` |
| `FUNCTIONS_EXTENSION_VERSION` | yes | `~4` |
| `WEBSITE_NODE_DEFAULT_VERSION` | yes | `~20` |
| `AzureWebJobsStorage` | yes | storage account connection string |
| `ENRICH_RATE_LIMIT_PER_MINUTE` | no | per-IP limit, default `30` |
| `ENRICH_GLOBAL_RATE_LIMIT_PER_MINUTE` | no | limit across all callers, default `300` |
| `AzureWebJobsFeatureFlags` | only on old hosts | `EnableWorkerIndexing`. Needed only if the Functions host predates 4.34; without it an older host will not discover v4-model functions. |

If `ANTHROPIC_API_KEY` is absent, the function logs `FATAL CONFIG: ...` at
startup and answers every request with 502 `upstream_error`. The key is never
logged and never appears in a response body.

## Running locally

Requires Node.js 20 or newer and Azure Functions Core Tools v4.

```
cd azure-function
npm ci
cp local.settings.json.example local.settings.json
npm start
```

Paste a real key into the copied `local.settings.json`. That file is
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
Function App spend cap and an Anthropic workspace budget are the hard ceiling.

## Cost

Claude Haiku 4.5 is billed at 1 USD per million input tokens and 5 USD per
million output tokens.

Per request the system prompt plus tool schema is about 660 tokens, and a
typical task adds 20 to 120 tokens, so roughly 700 to 800 input tokens. The
forced tool call is about 40 to 60 output tokens.

```
input   750 tokens x 1 USD / 1M = 0.00075 USD
output   50 tokens x 5 USD / 1M = 0.00025 USD
total                             about 0.001 USD per request
```

That is roughly 1000 enrichments per USD, so a user creating 20 tasks a day
costs about 0.60 USD a month in model spend. Prompt caching is not used: the
cacheable prefix here is below Anthropic's minimum cacheable prefix length, so
it would have no effect.

Consumption-plan hosting adds effectively nothing at this volume beyond the
storage account. The real cost risk is not the per-request price, it is
someone replaying the embedded function key, which is what the limiter and a
spend cap exist for.

## Privacy

The task text is user personal data. It is sent to the Anthropic API and is
never written to logs. Each invocation logs one JSON line containing only the
outcome, HTTP status, duration, input length, resulting category and token
counts.
