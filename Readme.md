# Pseudorandomometric Sequences API

A simple, self-hosted REST API for generating sequences of pseudo-random numbers from various statistical probability distributions. Built with Kotlin, Spring Boot, and the [Apache Commons Math](https://commons.apache.org/proper/commons-math/) library.

## Documentation

* **Official Docs:** [pseudorandomometricsequences.redriottank.com](https://pseudorandomometricsequences.redriottank.com)
* **Swagger UI (Live API):** [pseudorandomometricsequences.redriottank.com/swagger](https://pseudorandomometricsequences.redriottank.com/swagger)

---

## 🚀 What It Does

This API provides a single endpoint to generate lists of floating-point numbers (`Double`) that follow a specific, well-known probability distribution. Instead of just getting uniformly random numbers (like `Math.random()`), you can request data that models real-world phenomena, such as heights (Gaussian), wait times (Exponential), or proportions (Beta).

### Key Features

* **Multiple Distributions:** Supports Uniform, Gaussian (Normal), Exponential, Gamma, Log-Normal, Beta, Weibull, Cauchy, T-Student, and Binomial distributions.
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
| `param3` | Double | (varies) | The third parameter (used only by specific distributions like `triangular`). |
---

## 📊 Supported Distributions

The `param1`, `param2`, and `param3` fields map to the standard parameters of each distribution. If omitted, they use common defaults.

| Distribution | param1 (Default) | param2 (Default) | param3 (Default) |
| :--- | :--- | :--- | :--- |
| **arcsine** | lower bound `a` (0.0) | upper bound `b` (1.0) | - |
| **argus** | endpoint `c` (1.0) | shape `χ` (1.0) | - |
| **bates** | count `n` [int] (12) | - | - |
| **benford** | - | - | - |
| **bernoulli** | probability `p` (0.5) | - | - |
| **beta** | shape `α` (1.0) | shape `β` (1.0) | - |
| **beta-binomial** | trials `n` [int] (10) | Beta shape `α` (1.0) | Beta shape `β` (1.0) |
| **beta-negative-binomial**| successes `r` [int] (1) | Beta shape `α` (2.0) | Beta shape `β` (1.0) |
| **binomial** | trials `n` [int] (10) | probability `p` (0.5) | - |
| **bose-einstein** | `ε/kT` (1.0) | - | - |
| **burr** | shape `c` (1.0) | shape `k` (1.0) | - |
| **cauchy** | location `x₀` (0.0) | scale `γ` (1.0) | - |
| **chi** | df `k` (1.0) | - | - |
| **chi-squared** | df `k` (1.0) | - | - |
| **conway-maxwell-poisson**| rate `λ` (2.0) | dispersion `ν` (1.0) | - |
| **crystal-ball** | transition `α` (1.5) | power index `n` (2.0) | - |
| **discrete-uniform** | lower `a` [int] (0) | upper `b` [int] (9) | - |
| **double-weibull** | shape `k` (2.0) | scale `λ` (1.0) | - |
| **erlang** | shape `k` [int] (1) | rate `λ` (1.0) | - |
| **exponential** | rate `λ` (1.0) | - | - |
| **f-distribution** | num df `d1` (5.0) | den df `d2` (5.0) | - |
| **fermi-dirac** | `ε/kT` (1.0) | - | - |
| **folded-normal** | mean `μ` (0.0) | std dev `σ` (1.0) | - |
| **frechet** | shape `α` (1.0) | scale `σ` (1.0) | location `μ` (0.0) |
| **gamma** | shape `k` (1.0) | scale `θ` (1.0) | - |
| **gaussian** | mean `μ` (0.0) | std dev `σ` (1.0) | - |
| **generalized-gamma** | scale `a` (1.0) | shape `d` (1.0) | power `p` (1.0) |
| **generalized-normal** | location `μ` (0.0) | scale `σ` (1.0) | shape `β` (2.0) |
| **generalized-pareto** | location `μ` (0.0) | scale `σ` (1.0) | shape `ξ` (0.0) |
| **geometric** | probability `p` (0.5) | - | - |
| **gev** | location `μ` (0.0) | scale `σ` (1.0) | shape `ξ` (0.0) |
| **gompertz** | shape `η` (1.0) | rate `b` (1.0) | - |
| **gumbel** | location `μ` (0.0) | scale `β` (1.0) | - |
| **half-cauchy** | scale `γ` (1.0) | - | - |
| **half-normal** | scale `σ` (1.0) | - | - |
| **hyperbolic-secant** | location `μ` (0.0) | scale `σ` (1.0) | - |
| **hypergeometric** | pop `N` [int] (100) | successes `K` [int] (50)| draws `n` [int] (10) |
| **inverse-gaussian** | mean `μ` (1.0) | shape `λ` (1.0) | - |
| **irwin-hall** | count `n` [int] (12) | - | - |
| **kumaraswamy** | shape `a` (2.0) | shape `b` (2.0) | - |
| **landau** | location `μ` (0.0) | scale `σ` (1.0) | - |
| **laplace** | location `μ` (0.0) | scale `b` (1.0) | - |
| **levy** | location `μ` (0.0) | scale `c` (1.0) | - |
| **log-laplace** | loc `μ` (0.0) | scale `b` (1.0) | - |
| **log-logistic** | scale `α` (1.0) | shape `β` (1.0) | - |
| **logarithmic** | probability `p` (0.5) | - | - |
| **logistic** | location `μ` (0.0) | scale `s` (1.0) | - |
| **lognormal** | log-mean `μ` (0.0) | log-sigma `σ` (1.0) | - |
| **lomax** | shape `α` (1.0) | scale `λ` (1.0) | - |
| **maxwell-boltzmann** | scale `σ` (1.0) | - | - |
| **maxwell-juttner** | temp `θ` (1.0) | - | - |
| **moyal** | location `μ` (0.0) | scale `σ` (1.0) | - |
| **nakagami** | shape `m` (1.0) | spread `Ω` (1.0) | - |
| **negative-binomial** | successes `r` [int] (1) | probability `p` (0.5) | - |
| **negative-hypergeo** | pop `N` [int] (50) | successes `K` [int] (25)| target `r` [int] (5) |
| **noncentral-chi-sq** | df `k` (3.0) | non-centrality `λ` (1.0)| - |
| **pareto** | scale `xm` (1.0) | shape `α` (1.0) | - |
| **planck** | temp `T` (1.0) | - | - |
| **poisson** | mean `λ` (1.0) | - | - |
| **rademacher** | - | - | - |
| **rayleigh** | scale `σ` (1.0) | - | - |
| **reciprocal** | lower `a` (0.1) | upper `b` (1.0) | - |
| **relativistic-breit-wigner** | pole mass `m₀` (1.0) | decay width `Γ` (0.1) | - |
| **rice** | non-centrality `ν` (0.0)| scale `σ` (1.0) | - |
| **scaled-inv-chi-sq** | df `ν` (3.0) | scale `τ²` (1.0) | - |
| **skellam** | Poisson `μ₁` (1.0) | Poisson `μ₂` (1.0) | - |
| **skew-normal** | location `ξ` (0.0) | scale `ω` (1.0) | shape `α` (0.0) |
| **slash** | location `μ` (0.0) | scale `σ` (1.0) | - |
| **t-student** | df `ν` (10.0) | - | - |
| **trapezoidal** | min `a` (0.0) | max `d` (1.0) | plateau `frac` (0.5) |
| **triangular** | min `a` (0.0) | mode `c` (0.5) | max `b` (1.0) |
| **truncated-normal** | mean `μ` (0.0) | std dev `σ` (1.0) | half-width `w` (3.0) |
| **tsallis** | temp `T` (0.1) | exponent `n` (7.0) | - |
| **tukey-lambda** | shape `λ` (0.0) | - | - |
| **uniform** | min `a` (0.0) | max `b` (1.0) | - |
| **voigt** | location `μ` (0.0) | Gaussian `σ` (1.0) | Lorentzian `γ` (0.5) |
| **von-mises** | mean dir `μ` (0.0) | concentration `κ` (1.0) | - |
| **weibull** | shape `k` (1.0) | scale `λ` (1.0) | - |
| **wigner-semicircle** | radius `R` (1.0) | - | - |
| **yule-simon** | shape `ρ` (1.5) | - | - |
| **zipf** | elements `n` [int] (10)| exponent `s` (1.0) | - |

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

2. **Run it on your own computer**: This project is open-source. You can download the latest release (or link your repo), modify the count validation limit in RandomService.kt, and run the API on your own machine for unlimited and private use.
