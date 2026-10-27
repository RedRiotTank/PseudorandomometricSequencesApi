#!/usr/bin/env python3
"""Render a 3D empirical density surface from PseudorandomometricSequencesApi.

The script asks the running API for samples from a bivariate normal distribution,
estimates the joint density on a grid, and renders it as a 3D surface with the
sample cloud projected onto the floor.

Example:
    python3 scripts/render_multivariate_normal_3d.py \
        --url http://localhost:8080 \
        --count 50000 \
        --seed 2026 \
        --output visualizations/multivariate_normal_3d.png

Requires: numpy, matplotlib.
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

import numpy as np

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
from matplotlib.ticker import MaxNLocator


def fetch_samples(
    base_url: str,
    count: int,
    seed: int | None,
    correlation: float,
) -> tuple[np.ndarray, dict]:
    covariance = [[1.0, correlation], [correlation, 1.0]]
    payload: dict = {
        "count": count,
        "type": "general",
        "distribution": "multivariate-normal",
        "mean": [0.0, 0.0],
        "covariance": covariance,
        "variableNames": ["X", "Y"],
    }
    if seed is not None:
        payload["seed"] = seed

    request = urllib.request.Request(
        f"{base_url.rstrip('/')}/api/v1/random/multivariate",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            result = json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise SystemExit(
            f"API returned HTTP {error.code}.\n{body}"
        ) from error
    except urllib.error.URLError as error:
        raise SystemExit(
            f"Could not reach {base_url}. Start the API first.\n{error}"
        ) from error

    samples = np.asarray(result["sequence"], dtype=float)
    if samples.ndim != 2 or samples.shape[1] != 2:
        raise SystemExit(f"Unexpected sample shape: {samples.shape}")

    return samples, result


def gaussian_smooth(array: np.ndarray, sigma_bins: float) -> np.ndarray:
    radius = max(1, int(4 * sigma_bins))
    offsets = np.arange(-radius, radius + 1, dtype=float)
    kernel = np.exp(-0.5 * (offsets / sigma_bins) ** 2)
    kernel /= kernel.sum()

    smoothed = np.apply_along_axis(
        lambda row: np.convolve(row, kernel, mode="same"), axis=1, arr=array
    )
    return np.apply_along_axis(
        lambda column: np.convolve(column, kernel, mode="same"), axis=0, arr=smoothed
    )


def estimate_density(
    samples: np.ndarray,
    bins: int,
    extent: float,
    sigma_bins: float,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    histogram, x_edges, y_edges = np.histogram2d(
        samples[:, 0],
        samples[:, 1],
        bins=bins,
        range=[[-extent, extent], [-extent, extent]],
    )
    histogram = gaussian_smooth(histogram, sigma_bins)

    x_centers = 0.5 * (x_edges[:-1] + x_edges[1:])
    y_centers = 0.5 * (y_edges[:-1] + y_edges[1:])
    dx = x_centers[1] - x_centers[0]
    dy = y_centers[1] - y_centers[0]

    density = histogram / (histogram.sum() * dx * dy)
    return x_centers, y_centers, density.T


def render(
    samples: np.ndarray,
    result: dict,
    output: Path,
    bins: int,
    extent: float,
    sigma_bins: float,
    correlation: float,
) -> None:
    x_centers, y_centers, density = estimate_density(
        samples, bins=bins, extent=extent, sigma_bins=sigma_bins
    )
    x_grid, y_grid = np.meshgrid(x_centers, y_centers)

    figure = plt.figure(figsize=(14, 10), facecolor="#070b14")
    axes = figure.add_subplot(111, projection="3d", facecolor="#070b14")

    axes.plot_surface(
        x_grid,
        y_grid,
        density,
        cmap="viridis",
        rstride=1,
        cstride=1,
        linewidth=0,
        antialiased=True,
        alpha=0.98,
        shade=True,
    )

    floor = 0.0
    axes.contourf(
        x_grid,
        y_grid,
        density,
        zdir="z",
        offset=floor,
        levels=28,
        cmap="viridis",
        alpha=0.32,
    )

    sample_count = min(len(samples), 5_000)
    sample_indices = np.random.default_rng(1234).choice(
        len(samples), size=sample_count, replace=False
    )
    axes.scatter(
        samples[sample_indices, 0],
        samples[sample_indices, 1],
        np.full(sample_count, floor),
        s=1.4,
        c="#f8fafc",
        alpha=0.2,
        depthshade=False,
    )

    axes.set_xlim(-extent, extent)
    axes.set_ylim(-extent, extent)
    axes.set_zlim(0, density.max() * 1.08)
    axes.set_box_aspect((1.0, 1.0, 0.58))
    axes.view_init(elev=27, azim=-47)

    axes.set_xlabel("X", color="#dbeafe", fontsize=12, labelpad=12)
    axes.set_ylabel("Y", color="#dbeafe", fontsize=12, labelpad=12)
    axes.set_zlabel("Density", color="#dbeafe", fontsize=12, labelpad=12)
    axes.tick_params(colors="#94a3b8", labelsize=9)
    axes.zaxis.set_major_locator(MaxNLocator(5))

    for axis in (axes.xaxis, axes.yaxis, axes.zaxis):
        axis.pane.set_facecolor((0.035, 0.055, 0.10, 0.96))
        axis.pane.set_edgecolor("#1e293b")
        axis._axinfo["grid"]["color"] = (0.25, 0.35, 0.50, 0.28)
        axis._axinfo["grid"]["linewidth"] = 0.6

    figure.suptitle(
        "Bivariate Normal Distribution",
        color="#f8fafc",
        fontsize=24,
        fontweight="bold",
        y=0.975,
    )

    seed_text = result.get("seed", "random")
    figure.text(
        0.5,
        0.927,
        f"ρ = {correlation:g}   ·   {len(samples):,} samples   ·   "
        f"empirical density   ·   seed {seed_text}",
        ha="center",
        va="top",
        color="#93c5fd",
        fontsize=12,
    )
    figure.text(
        0.5,
        0.035,
        "Generated with PseudorandomometricSequencesApi  ·  "
        "POST /api/v1/random/multivariate",
        ha="center",
        va="bottom",
        color="#64748b",
        fontsize=10,
    )

    colorbar = figure.colorbar(
        plt.cm.ScalarMappable(
            cmap="viridis",
            norm=Normalize(vmin=0.0, vmax=float(density.max())),
        ),
        ax=axes,
        shrink=0.62,
        pad=0.075,
        aspect=18,
    )
    colorbar.ax.tick_params(colors="#94a3b8", labelsize=9)
    colorbar.outline.set_edgecolor("#334155")

    figure.subplots_adjust(top=0.89, bottom=0.06)

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
        f"Samples: {len(samples):,} | dimension: {result.get('dimension')} | "
        f"distribution: {result.get('distribution')} | seed: {result.get('seed')}"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--url",
        default="http://localhost:8080",
        help="Base URL of the running API (default: http://localhost:8080)",
    )
    parser.add_argument("--count", type=int, default=50_000, help="Number of samples")
    parser.add_argument("--seed", type=int, default=2026, help="API seed (default: 2026)")
    parser.add_argument(
        "--correlation",
        type=float,
        default=0.8,
        help="Off-diagonal correlation in [-1, 1] (default: 0.8)",
    )
    parser.add_argument("--bins", type=int, default=80, help="Density grid bins")
    parser.add_argument("--extent", type=float, default=4.0, help="Plot half-range")
    parser.add_argument(
        "--sigma-bins",
        type=float,
        default=1.8,
        help="Gaussian smoothing sigma in bins",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("visualizations/multivariate_normal_3d.png"),
        help="Output PNG path",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if not 1 <= args.count <= 2_000_000:
        raise SystemExit("--count must be between 1 and 2,000,000")
    if not -0.999 <= args.correlation <= 0.999:
        raise SystemExit("--correlation must be between -0.999 and 0.999")

    samples, result = fetch_samples(
        base_url=args.url,
        count=args.count,
        seed=args.seed,
        correlation=args.correlation,
    )
    render(
        samples=samples,
        result=result,
        output=args.output,
        bins=args.bins,
        extent=args.extent,
        sigma_bins=args.sigma_bins,
        correlation=args.correlation,
    )


if __name__ == "__main__":
    sys.exit(main())
