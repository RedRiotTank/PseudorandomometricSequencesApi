# Module PseudorandomometricSequencesApi

A simple, self-hosted REST API for generating sequences of pseudo-random numbers from various statistical probability distributions. Built with Kotlin, Spring Boot, and the [Apache Commons Math](https://commons.apache.org/proper/commons-math/) library.

## Documentation

* **Official Docs:** [pseudorandomometricsequences.redriottank.com](https://pseudorandomometricsequences.redriottank.com)
* **Swagger UI (Live API):** [pseudorandomometricsequences.redriottank.com/swagger](https://pseudorandomometricsequences.redriottank.com/swagger)

---

## 🚀 What It Does

This API provides a single endpoint to generate lists of floating-point numbers (`Double`) that follow a specific, well-known probability distribution. Instead of just getting uniformly random numbers (like `Math.random()`), you can request data that models real-world phenomena, such as heights (Gaussian), wait times (Exponential), or proportions (Beta).

### Key Features

* **Multiple Distributions:** Supports Uniform, Gaussian (Normal), Exponential, Gamma, Log-Normal, Beta, Weibull, Cauchy, T-Student, and Binomial distributions.
* **Joint Multivariate Distributions:** Generate correlated vectors from multivariate normal, multivariate t, Dirichlet, and multinomial distributions.
* **Two Generator Types:**
    * `general`: Uses `java.util.Random`, a standard, fast pseudo-random number generator (PRNG).
    * `secure`: Uses `java.security.SecureRandom`, a cryptographically strong PRNG (slower but less predictable).
* **Parameterizable:** You can specify the key parameters for each distribution (e.g., mean/std. dev for Gaussian).

### Common Use Cases

* **Simulation:** Powering Monte Carlo simulations.
* **Data Science:** Creating synthetic datasets for testing machine learning models.
* **Testing:** Generating realistic load for performance testing (e.g., simulating user behavior).
* **Game Development:** Modeling events like loot drops, damage rolls, or AI behavior.
* **Education:** Visualizing and understanding different probability distributions.

---

## 🛠️ API Endpoint

### `GET /api/v1/random/sequence`

Generates a sequence of pseudo-random numbers based on the provided query parameters.

#### Query Parameters

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `count` | Integer | `10` | The number of samples to generate. **Max: 2,000,000**. |
| `type` | String | `general` | The base generator type. Options: `general` or `secure`. |
| `distribution` | String | `uniform` | The probability distribution to sample from. |
| `param1` | Double | (varies) | The first parameter for the distribution (see table below). |
| `param2` | Double | (varies) | The second parameter for the distribution (see table below). |

---

## 📊 Supported Distributions

The `param1` and `param2` fields map to the standard parameters of each distribution. If omitted, they use common defaults.

| `distribution` | `param1` (Default) | `param2` (Default) | Description |
| :--- | :--- | :--- | :--- |
| **`uniform`** | `min` (0.0) | `max` (1.0) | Uniform distribution U(a, b). |
| **`gaussian`** | `mean` (&mu;) (0.0) | `std. dev` (&sigma;) (1.0) | Normal distribution N(&mu;, &sigma;&sup2;). |
| **`exponential`** | `mean` (1/&lambda;) (1.0) | *N/A* | Exponential distribution with a given mean. |
| **`gamma`** | `shape` (k) (1.0) | `scale` (&theta;) (1.0) | Gamma distribution &Gamma;(k, &theta;). |
| **`lognormal`** | `scale` (&mu;) (0.0) | `shape` (&sigma;) (1.0) | Log-normal (mean &mu; and std. dev &sigma; of the *underlying* normal). |
| **`beta`** | `alpha` (&alpha;) (1.0) | `beta` (&beta;) (1.0) | Beta distribution Beta(&alpha;, &beta;). |
| **`weibull`** | `shape` (&alpha;) (1.0) | `scale` (&beta;) (1.0) | Weibull distribution W(&alpha;, &beta;). |
| **`cauchy`** | `median` (x&#8320;) (0.0) | `scale` (&gamma;) (1.0) | Cauchy distribution C(x&#8320;, &gamma;). |
| **`tstudent`** | `d. of freedom` (&nu;) (1.0) | *N/A* | Student's t-distribution. `param1` must be > 0. |
| **`binomial`** | `trials` (n) (1.0) | `probability` (p) (0.5) | Binomial B(n, p). `param1` is truncated to an integer (n &ge; 0). |

---

## 📋 Examples

Using `curl` from the command line.

**Example 1: Get 5 numbers from a Gaussian (Normal) distribution with a mean of 100 and std. dev of 15.**
```bash
curl "[https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=5&distribution=gaussian&param1=100&param2=15](https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=5&distribution=gaussian&param1=100&param2=15)"
```

```json
{
  "type": "general",
  "count": 5,
  "distribution": "gaussian",
  "sequence": [
    98.138,
    115.221,
    89.445,
    103.002,
    122.910
  ]
}
```

**Example 2: Get 3 numbers from a Beta distribution with α=2 and β=5.**

```bash
curl "[https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=3&distribution=beta&param1=2&param2=5](https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=3&distribution=beta&param1=2&param2=5)"
```
```json
{
  "type": "general",
  "count": 3,
  "distribution": "beta",
  "sequence": [
    0.345,
    0.199,
    0.401
  ]
}
```
**Example 3: Get 4 numbers from an Exponential distribution using the secure generator.**

```bash
curl "[https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=4&distribution=exponential&type=secure](https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequence?count=4&distribution=exponential&type=secure)"
```
```json
{
  "type": "secure",
  "count": 4,
  "distribution": "exponential",
  "sequence": [
    0.891,
    1.422,
    0.310,
    2.109
  ]
}
```

## 🧮 Independent Multi-Variable Generation

> **These variables are independent.** This endpoint bundles several univariate sequences into
> one response, but it does not create correlations between them. For statistically dependent
> components, use [Joint Multivariate Generation](#-joint-multivariate-generation) below.

### `POST /api/v1/random/sequences`

Generates multiple named variables in a single request. Each variable can use its own
distribution and parameters. When a request-level `seed` is provided, every variable gets a
deterministically derived seed, so the whole request is reproducible. A variable-level `seed`
overrides its derived seed and makes that variable reproducible on its own.

#### Request Body

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `count` | Integer | `10` | Samples per variable. **Total samples across all variables: max 2,000,000**. |
| `type` | String | `general` | Base generator type: `general` or `secure`. |
| `seed` | Long | (random) | Request-level seed used to derive each variable's seed. |
| `variables` | Array | (required) | One or more variable definitions. |
| `variables[].name` | String | `variable1`, ... | Optional unique variable name. |
| `variables[].distribution` | String | (required) | Distribution to sample from. |
| `variables[].param1` | Double | (varies) | First distribution parameter. |
| `variables[].param2` | Double | (varies) | Second distribution parameter. |
| `variables[].param3` | Double | (varies) | Third distribution parameter. |
| `variables[].seed` | Long | (derived) | Optional seed for this variable only. |

#### Example: generate temperature and humidity together

```bash
curl -X POST "https://pseudorandomometricsequences.redriottank.com/api/v1/random/sequences" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 3,
    "type": "general",
    "seed": 42,
    "variables": [
      {"name": "temperature", "distribution": "gaussian", "param1": 20, "param2": 2},
      {"name": "humidity", "distribution": "uniform", "param1": 30, "param2": 60}
    ]
  }'
```

```json
{
  "type": "general",
  "count": 3,
  "seed": 42,
  "variables": [
    {
      "name": "temperature",
      "distribution": "gaussian",
      "seed": -5025562857975149833,
      "sequence": [18.349, 14.874, 18.758]
    },
    {
      "name": "humidity",
      "distribution": "uniform",
      "seed": -5843495416241995736,
      "sequence": [36.582, 31.589, 54.264]
    }
  ]
}
```

The existing single-variable `GET /api/v1/random/sequence` endpoint remains fully supported.

## 🔗 Joint Multivariate Generation

### `POST /api/v1/random/multivariate`

Generates vector-valued observations from a joint distribution. Unlike the independent
multi-variable endpoint, the values inside each vector may be statistically dependent. The
response contains one array per observation, in the order given by `variableNames`.

`GET /api/v1/distributions/multivariate` lists the available joint distributions.

#### Supported joint distributions

| Distribution | Required fields | Description |
| :--- | :--- | :--- |
| `multivariate-normal` | `covariance` (optional `mean`) | Joint normal N(μ, Σ). Non-zero off-diagonal covariance produces correlated components. |
| `multivariate-t` | `covariance`, `degreesOfFreedom` (optional `mean`) | Joint Student's t with scale matrix Σ and ν > 0. |
| `dirichlet` | `concentration` | Probability vectors whose components are non-negative and sum to 1. |
| `multinomial` | `trials`, `probabilities` | Count vectors whose components sum to `trials`. |

#### Request Body

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `count` | Integer | `10` | Number of vector observations. |
| `type` | String | `general` | Base generator type: `general` or `secure`. |
| `seed` | Long | (random) | Seed used to reproduce the sequence. |
| `distribution` | String | (required) | Joint distribution name. |
| `mean` | Double[] | zeros | Mean/location vector for `multivariate-normal` and `multivariate-t`. |
| `covariance` | Double[][] | (required for normal/t) | Symmetric positive-definite covariance/scale matrix. |
| `degreesOfFreedom` | Double | (required for t) | ν > 0. |
| `concentration` | Double[] | (required for dirichlet) | Concentration vector α, all values > 0. |
| `trials` | Integer | (required for multinomial) | Number of trials per observation. |
| `probabilities` | Double[] | (required for multinomial) | Category probabilities, non-negative and summing to 1. |
| `variableNames` | String[] | `x1`, `x2`, ... | Optional component names; length must match the dimension. |

**Limits:** `count` must be positive, and `count × dimension` must not exceed 2,000,000.

#### Example: correlated bivariate normal

```bash
curl -X POST "https://pseudorandomometricsequences.redriottank.com/api/v1/random/multivariate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 3,
    "seed": 42,
    "distribution": "multivariate-normal",
    "mean": [0.0, 0.0],
    "covariance": [[1.0, 0.8], [0.8, 1.0]],
    "variableNames": ["height", "weight"]
  }'
```

```json
{
  "type": "general",
  "count": 3,
  "distribution": "multivariate-normal",
  "dimension": 2,
  "seed": 42,
  "variableNames": ["height", "weight"],
  "sequence": [
    [1.4325228983022713, 1.3330190510448872],
    [-1.3446476540230368, -1.4149154862577822],
    [0.5574866615513397, 0.7380022637698389]
  ]
}
```

The off-diagonal `0.8` in the covariance matrix means the two components are positively
correlated. Set it to `0.0` for independent components.

#### Example: Dirichlet probability vectors

```bash
curl -X POST "https://pseudorandomometricsequences.redriottank.com/api/v1/random/multivariate" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 2,
    "seed": 7,
    "distribution": "dirichlet",
    "concentration": [2.0, 5.0, 3.0]
  }'
```

## ⚠️ Important Disclaimers & Usage Limits

**Service Availability**

This is a self-hosted service provided for free. It is not a high-availability, enterprise-grade API and may experience downtime or interruptions without notice.

**Request Throttling**

Please avoid aggressive polling or sending a large number of requests in a short period. Users who abuse the service (e.g., in a way that risks a DDoS) may have their IP address rate-limited or banned to protect the stability of the service for others.

**Generation Limit**
The API has a hard limit of 2,000,000 (2 million) numbers per single request.

If you require a larger dataset or more consistent availability, you have two options:

1. **Contact me** to discuss your use case. You can do it through my email albertoplazamontesdm@gmail.com

2. **Run it on your own computer**: This project is open-source. You can download the latest release (o enlaza tu repo), modify the count validation limit in RandomService.kt, and run the API on your own machine for unlimited and private use.
