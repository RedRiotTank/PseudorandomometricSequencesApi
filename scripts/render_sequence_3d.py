#!/usr/bin/env python3
"""Render any PseudorandomometricSequencesApi sequence as a 3D visualization.

Continuous distributions are drawn as a smooth empirical density surface;
discrete distributions are detected automatically and drawn as 3D probability
bars. The script can either ask a running API for a fresh sequence or read a
saved JSON response. It works with every univariate distribution supported by
the API, so the same command can visualize a Gaussian, a Poisson, a Cauchy, etc.

Examples:
    # Generate a Gaussian sequence from the API and render it.
    python3 scripts/render_sequence_3d.py \
        --url http://localhost:8080 \
        --distribution gaussian --param1 0 --param2 1 \
        --count 50000 --seed 2026 \
        --output visualizations/gaussian_3d.png

    # Render an already-saved response (plain array, /sequence or /sequences).
    python3 scripts/render_sequence_3d.py \
        --input response.json \
        --output visualizations/saved_sequence_3d.png

Requires: numpy, matplotlib.
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

import numpy as np

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.colors import Normalize


def fetch_samples(args: argparse.Namespace) -> tuple[np.ndarray, dict]:
    query: dict[str, str] = {
        "count": str(args.count),
        "type": args.type,
        "distribution": args.distribution,
    }
    for name in ("param1", "param2", "param3"):
        value = getattr(args, name)
        if value is not None:
            query[name] = repr(value)
    if args.seed is not None:
        query["seed"] = str(args.seed)

    url = f"{args.url.rstrip('/')}/api/v1/random/sequence?{urllib.parse.urlencode(query)}"
    request = urllib.request.Request(url, method="GET")

    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            result = json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"API returned HTTP {error.code}.\n{body}") from error
    except urllib.error.URLError as error:
        raise SystemExit(
            f"Could not reach {args.url}. Start the API first or use --input.\n{error}"
        ) from error

    return np.asarray(result["sequence"], dtype=float), result


def load_samples(path: Path, variable: str | None) -> tuple[np.ndarray, dict]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except OSError as error:
        raise SystemExit(f"Could not read {path}: {error}") from error
    except json.JSONDecodeError as error:
        raise SystemExit(f"{path} is not valid JSON: {error}") from error

    if isinstance(payload, list):
        return np.asarray(payload, dtype=float), {"distribution": path.stem, "seed": "n/a"}

    if not isinstance(payload, dict):
        raise SystemExit("The input JSON must be an array or an object.")

    if "variables" in payload:
        variables = payload["variables"]
        if not isinstance(variables, list) or not variables:
            raise SystemExit("The 'variables' field must be a non-empty array.")

        if variable is not None:
            matching = [item for item in variables if item.get("name") == variable]
            if not matching:
                names = ", ".join(str(item.get("name")) for item in variables)
                raise SystemExit(f"Variable '{variable}' not found. Available: {names}")
            selected = matching[0]
        elif len(variables) == 1:
            selected = variables[0]
        else:
            names = ", ".join(str(item.get("name")) for item in variables)
            raise SystemExit(
                f"The response contains several variables. Choose one with --variable. Available: {names}"
            )

        metadata = dict(payload)
        metadata.update(selected)
        return np.asarray(selected["sequence"], dtype=float), metadata

    if "sequence" in payload:
        return np.asarray(payload["sequence"], dtype=float), payload

    raise SystemExit("Could not find a 'sequence' or 'variables' field in the input JSON.")


def gaussian_smooth(array: np.ndarray, sigma_bins: float) -> np.ndarray:
    if sigma_bins <= 0:
        return array

    radius = max(1, int(4 * sigma_bins))
    offsets = np.arange(-radius, radius + 1, dtype=float)
    kernel = np.exp(-0.5 * (offsets / sigma_bins) ** 2)
    kernel /= kernel.sum()
    return np.convolve(array, kernel, mode="same")


def estimate_density(
    samples: np.ndarray,
    bins: int,
    sigma_bins: float,
) -> tuple[np.ndarray, np.ndarray, float, float, bool, np.ndarray | None]:
    finite = samples[np.isfinite(samples)]
    if finite.size == 0:
        raise SystemExit("The sequence does not contain finite values.")

    minimum = float(np.min(finite))
    maximum = float(np.max(finite))
    integer_like = (
        abs(minimum) <= 1e12
        and abs(maximum) <= 1e12
        and maximum - minimum <= 80
        and np.all(np.abs(finite - np.round(finite)) < 1e-9)
    )

    if integer_like:
        low = int(np.round(minimum))
        high = int(np.round(maximum))
        support = high - low + 1
        counts = np.zeros(support, dtype=float)
        indices = np.round(finite).astype(int) - low
        np.add.at(counts, indices, 1.0)
        density = counts / finite.size
        centers = np.arange(low, high + 1, dtype=float)
        return centers, density, low - 0.5, high + 0.5, True, centers

    lo, hi = np.percentile(finite, [0.5, 99.5])
    if not np.isfinite(lo) or not np.isfinite(hi) or hi <= lo:
        center = float(np.nanmean(finite))
        padding = max(0.5, abs(center) * 0.05)
        lo, hi = center - padding, center + padding

    counts, edges = np.histogram(finite, bins=bins, range=(lo, hi))
    width = (hi - lo) / bins
    density = gaussian_smooth(counts.astype(float), sigma_bins)
    density = density / max(1, finite.size) / width
    centers = 0.5 * (edges[:-1] + edges[1:])

    return centers, density, float(lo), float(hi), False, None


def pretty_distribution(name: str | None) -> str:
    if not name:
        return "Sequence"
    words = [word for word in name.replace("_", "-").split("-") if word]
    special = {"gev": "GEV", "f": "F", "t": "T"}
    return " ".join(special.get(word.lower(), word.capitalize()) for word in words)


def render(
    samples: np.ndarray,
    metadata: dict,
    output: Path,
    distribution: str | None,
    bins: int,
    sigma_bins: float,
    title: str | None,
) -> None:
    finite = samples[np.isfinite(samples)]
    centers, density, lo, hi, is_discrete, _ = estimate_density(
        samples, bins=bins, sigma_bins=sigma_bins
    )
    max_density = float(density.max()) if density.size else 1.0
    if max_density <= 0:
        max_density = 1.0

    figure = plt.figure(figsize=(14, 10), facecolor="#070b14")
    axes = figure.add_axes([0.02, 0.075, 0.96, 0.855], projection="3d", facecolor="#070b14")

    bar_width = (hi - lo) / len(centers)
    gap = 0.16 if is_discrete else 0.03
    depth = 0.32
    if is_discrete:
        bar_depth = depth * (1.0 - gap)
        colors = plt.cm.viridis(Normalize(vmin=0.0, vmax=max_density)(density))
        axes.bar3d(
            centers - bar_width * (1.0 - gap) / 2.0,
            np.full_like(centers, depth * gap / 2.0),
            np.zeros_like(centers),
            bar_width * (1.0 - gap),
            np.full_like(centers, bar_depth),
            density,
            color=colors,
            shade=True,
            edgecolor="none",
            alpha=1.0,
        )
    else:
        x_grid, y_grid = np.meshgrid(centers, np.array([0.0, depth]))
        z_grid = np.vstack([density, density])
        axes.plot_surface(
            x_grid,
            y_grid,
            z_grid,
            cmap="viridis",
            vmin=0.0,
            vmax=max_density,
            rstride=1,
            cstride=1,
            linewidth=0,
            antialiased=True,
            shade=True,
            alpha=0.98,
        )

    # Density outline on the back wall.
    axes.plot(centers, np.full_like(centers, depth), density, color="#7dd3fc", linewidth=1.6)
    axes.plot(
        centers,
        np.full_like(centers, depth),
        np.zeros_like(centers),
        color=(0.29, 0.45, 0.65, 0.35),
        linewidth=1.0,
    )

    # Project the sample cloud onto the floor.
    rng = np.random.default_rng(1234)
    sample_count = min(finite.size, 6_000)
    chosen = rng.choice(finite.size, size=sample_count, replace=False)
    clipped = np.clip(finite[chosen], lo, hi)
    axes.scatter(
        clipped,
        rng.random(sample_count) * depth,
        np.zeros(sample_count),
        s=1.2,
        c="#f8fafc",
        alpha=0.18,
        depthshade=False,
    )

    axes.set_xlim(lo, hi)
    axes.set_ylim(0, depth)
    axes.set_zlim(0, max_density * 1.08)
    try:
        axes.set_box_aspect((1.35, 0.40, 0.58), zoom=1.22)
    except TypeError:  # older Matplotlib without the zoom argument
        axes.set_box_aspect((1.35, 0.40, 0.58))
    axes.view_init(elev=26, azim=-52)

    axes.xaxis.set_rotate_label(False)
    axes.set_xlabel("Value", color="#dbeafe", fontsize=12, labelpad=14, rotation=0)
    axes.set_ylabel("")
    axes.set_zlabel("")
    axes.set_yticks([])
    axes.set_zticklabels([])
    axes.tick_params(axis="x", colors="#94a3b8", labelsize=9)

    for axis in (axes.xaxis, axes.yaxis, axes.zaxis):
        axis.pane.set_facecolor((0.035, 0.055, 0.10, 0.96))
        axis.pane.set_edgecolor("#1e293b")
        axis._axinfo["grid"]["color"] = (0.25, 0.35, 0.50, 0.28)
        axis._axinfo["grid"]["linewidth"] = 0.6

    distribution_name = distribution or metadata.get("distribution")
    heading = title or f"{pretty_distribution(distribution_name)} Distribution"
    figure.suptitle(
        heading,
        color="#f8fafc",
        fontsize=24,
        fontweight="bold",
        y=0.975,
    )

    params = [
        f"param{index} = {metadata.get(f'param{index}'):g}"
        for index in (1, 2, 3)
        if isinstance(metadata.get(f"param{index}"), (int, float))
    ]
    parameter_text = distribution_name or "sequence"
    if params:
        parameter_text += "   ·   " + "   ·   ".join(params)

    mean = float(np.mean(finite))
    std = float(np.std(finite))
    seed = metadata.get("seed", "n/a")
    figure.text(
        0.5,
        0.936,
        parameter_text,
        ha="center",
        va="top",
        color="#93c5fd",
        fontsize=12,
    )
    figure.text(
        0.5,
        0.907,
        f"n = {finite.size:,}   ·   mean = {mean:.3g}   ·   sd = {std:.3g}   ·   seed {seed}",
        ha="center",
        va="top",
        color="#94a3b8",
        fontsize=11,
    )
    figure.text(
        0.5,
        0.035,
        "Generated with PseudorandomometricSequencesApi  ·  "
        "GET /api/v1/random/sequence",
        ha="center",
        va="bottom",
        color="#64748b",
        fontsize=10,
    )

    colorbar = figure.colorbar(
        plt.cm.ScalarMappable(cmap="viridis", norm=Normalize(vmin=0.0, vmax=max_density)),
        ax=axes,
        shrink=0.62,
        pad=0.075,
        aspect=18,
    )
    colorbar.ax.tick_params(colors="#94a3b8", labelsize=9)
    colorbar.outline.set_edgecolor("#334155")
    colorbar.set_label(
        "Probability" if is_discrete else "Density",
        color="#dbeafe",
        fontsize=11,
    )

    output.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(
        output,
        dpi=200,
        facecolor=figure.get_facecolor(),
        bbox_inches="tight",
    )
    plt.close(figure)

    print(f"Wrote {output} ({output.stat().st_size / 1024:.0f} KiB)")
    print(
        f"Samples: {finite.size:,} | distribution: {distribution_name} | "
        f"seed: {metadata.get('seed', 'n/a')} | range: [{lo:.4g}, {hi:.4g}]"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--url",
        default="http://localhost:8080",
        help="Base URL of the running API (default: http://localhost:8080)",
    )
    parser.add_argument(
        "--distribution",
        default="gaussian",
        help="Distribution to request from the API (default: gaussian)",
    )
    parser.add_argument("--param1", type=float, default=None, help="First distribution parameter")
    parser.add_argument("--param2", type=float, default=None, help="Second distribution parameter")
    parser.add_argument("--param3", type=float, default=None, help="Third distribution parameter")
    parser.add_argument("--count", type=int, default=50_000, help="Number of samples (default: 50000)")
    parser.add_argument(
        "--type",
        choices=("general", "secure"),
        default="general",
        help="Base generator type (default: general)",
    )
    parser.add_argument("--seed", type=int, default=2026, help="API seed (default: 2026)")
    parser.add_argument(
        "--input",
        type=Path,
        default=None,
        help="Read a saved JSON response instead of calling the API",
    )
    parser.add_argument(
        "--variable",
        default=None,
        help="Variable name to use when --input contains a /sequences response",
    )
    parser.add_argument("--bins", type=int, default=64, help="Histogram bins (default: 64)")
    parser.add_argument(
        "--sigma-bins",
        type=float,
        default=1.3,
        help="Gaussian smoothing sigma in bins (default: 1.3)",
    )
    parser.add_argument("--title", default=None, help="Optional custom image title")
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output PNG path (default: visualizations/<distribution>_3d.png)",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if not 1 <= args.count <= 2_000_000:
        raise SystemExit("--count must be between 1 and 2,000,000")
    if not 8 <= args.bins <= 200:
        raise SystemExit("--bins must be between 8 and 200")

    if args.input is not None:
        samples, metadata = load_samples(args.input, args.variable)
    else:
        samples, metadata = fetch_samples(args)
        for name in ("param1", "param2", "param3"):
            value = getattr(args, name)
            if value is not None:
                metadata[name] = value

    output = args.output
    if output is None:
        source_name = args.distribution if args.input is None else metadata.get("distribution", "sequence")
        name = str(source_name or "sequence").replace("/", "-")
        output = Path("visualizations") / f"{name}_3d.png"

    render(
        samples=samples,
        metadata=metadata,
        output=output,
        distribution=None if args.input is not None else args.distribution,
        bins=args.bins,
        sigma_bins=args.sigma_bins,
        title=args.title,
    )


if __name__ == "__main__":
    sys.exit(main())
