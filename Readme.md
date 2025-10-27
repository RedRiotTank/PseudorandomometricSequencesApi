# Pseudorandomometric Sequences API

A simple, self-hosted REST API for generating sequences of pseudo-random numbers from various statistical probability distributions.

## Documentation

* **Official Docs:** [pseudorandomometricsequences.redriottank.com](https://pseudorandomometricsequences.redriottank.com)
* **Swagger UI (Live API):** [pseudorandomometricsequences.redriottank.com/swagger](https://pseudorandomometricsequences.redriottank.com/swagger)

---

## 🚀 What It Does

This API provides a single endpoint to generate lists of floating-point numbers (`Double`) that follow a specific, well-known probability distribution. Instead of just getting uniformly random numbers (like `Math.random()`), you can request data that models real-world phenomena, such as heights (Gaussian), wait times (Exponential), or proportions (Beta).

### Key Features

* **Multiple Distributions:** Supports Uniform, Gaussian (Normal), Exponential, Gamma, Log-Normal, and Beta distributions.
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

## ⚠️ Important Disclaimers & Usage Limits

**Service Availability**

This is a self-hosted service provided for free. It is not a high-availability, enterprise-grade API and may experience downtime or interruptions without notice.

**Request Throttling**

Please avoid aggressive polling or sending a large number of requests in a short period. Users who abuse the service (e.g., in a way that risks a DDoS) may have their IP address rate-limited or banned to protect the stability of the service for others.

**Generation Limit**
The API has a hard limit of 2,000,000 (2 million) numbers per single request.

If you require a larger dataset or more consistent availability, you have two options:

1. **Contact me** to discuss your use case. You can do it through my email albertoplazamontesdm@gmail.com

2. **Run it on your own computer**: This project is open-source. You can download the latest release, launch the app with the count validation limit you need, and run the API on your own machine for unlimited and private use.
