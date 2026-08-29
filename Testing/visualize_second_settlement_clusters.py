"""
For the article's "Finding the Second Settlement" section: renders the SAME map under
the 3 top-ranked strategies, side by side. Each panel shows:
  - hex borders colored by connected component (cluster) - so cluster boundaries (one
    big cluster vs. several smaller ones) are visible at a glance
  - town circles at their real geometric position, colored by that strategy's own
    FinalOptLocation (matching the article's usual town-heatmap style), with the best
    town highlighted

(The per-tile 0-1 ranking heatmap itself was already shown earlier - this is deliberately
NOT repeated here; hexes just carry a light resource-only fill so the cluster borders and
town colors stay the focus.)

Usage: python visualize_second_settlement_clusters.py
Requires: a JDK on PATH (javac/java), matplotlib.
"""

import math
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import RegularPolygon

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = PROJECT_ROOT.parent
BUILD_DIR = PROJECT_ROOT / "testing" / "_build"
OUTPUT_DIR = PROJECT_ROOT / "testing" / "_second_settlement_clusters"

RESOURCE_NAMES = {0: "Brick", 1: "Ore", 2: "Sheep", 3: "Wheat", 4: "Wood", 5: "Desert"}

HEX_TO_TOWNS_MAP = [
    [0,3,4,7,8,12],[1,4,5,8,9,13],[2,5,6,9,10,14],
    [7,11,12,16,17,22],[8,12,13,17,18,23],[9,13,14,18,19,24],[10,14,15,19,20,25],
    [16,21,22,27,28,33],[17,22,23,28,29,34],[18,23,24,29,30,35],[19,24,25,30,31,36],[20,25,26,31,32,37],
    [28,33,34,38,39,43],[29,34,35,39,40,44],[30,35,36,40,41,45],[31,36,37,41,42,46],
    [39,43,44,47,48,51],[40,44,45,48,49,52],[41,45,46,49,50,53],
]

def _build_hex_centers():
    rows = [[0,1,2],[3,4,5,6],[7,8,9,10,11],[12,13,14,15],[16,17,18]]
    size = 1.0; horiz = math.sqrt(3)*size; vert = 1.5*size
    centers = {}
    for r, row in enumerate(rows):
        L = len(row)
        for i, h in enumerate(row):
            centers[h] = ((i - (L-1)/2)*horiz, -r*vert)
    return centers

HEX_CENTERS = _build_hex_centers()
HEX_SIZE = 1.0

def _hex_corner_list(cx, cy):
    return [(round(cx + HEX_SIZE*math.cos(math.radians(60*i-30)), 4),
             round(cy + HEX_SIZE*math.sin(math.radians(60*i-30)), 4)) for i in range(6)]

HEX_CORNERS = {h: _hex_corner_list(*HEX_CENTERS[h]) for h in range(19)}

def _solve_town_positions():
    town_hexes = defaultdict(list)
    for h in range(19):
        for t in HEX_TO_TOWNS_MAP[h]:
            town_hexes[t].append(h)

    town_position = {}
    claimed = {h: set() for h in range(19)}
    remaining = []
    for t in range(54):
        hexes = town_hexes[t]
        if len(hexes) == 3:
            common = set(HEX_CORNERS[hexes[0]]) & set(HEX_CORNERS[hexes[1]]) & set(HEX_CORNERS[hexes[2]])
            pos = next(iter(common))
            town_position[t] = pos
            for h in hexes: claimed[h].add(pos)
        else:
            remaining.append(t)

    changed = True
    while changed and remaining:
        changed = False
        still = []
        for t in remaining:
            hexes = town_hexes[t]
            if len(hexes) == 2:
                shared = list(set(HEX_CORNERS[hexes[0]]) & set(HEX_CORNERS[hexes[1]]))
                free = [c for c in shared if c not in claimed[hexes[0]] and c not in claimed[hexes[1]]]
                if len(free) == 1:
                    town_position[t] = free[0]
                    for h in hexes: claimed[h].add(free[0])
                    changed = True
                    continue
            still.append(t)
        remaining = still

    still = []
    for t in remaining:
        h = town_hexes[t][0]
        free = [c for c in HEX_CORNERS[h] if c not in claimed[h]]
        if len(free) == 1:
            town_position[t] = free[0]
            claimed[h].add(free[0])
        else:
            still.append(t)
    remaining = still

    for h in range(19):
        free_corners = [c for c in HEX_CORNERS[h] if c not in claimed[h]]
        free_towns = sorted(t for t in HEX_TO_TOWNS_MAP[h] if t not in town_position)
        if not free_towns: continue
        cx, cy = HEX_CENTERS[h]
        free_corners.sort(key=lambda c: math.atan2(c[1]-cy, c[0]-cx))
        for t, pos in zip(free_towns, free_corners):
            town_position[t] = pos
            claimed[h].add(pos)

    assert len(town_position) == 54 and len(set(town_position.values())) == 54
    return town_position

TOWN_POSITIONS = _solve_town_positions()

# a qualitative palette for cluster borders - enough distinct colors for any realistic
# number of connected components on a 19-tile board
CLUSTER_COLORS = [
    "#1f77b4", "#2ca02c", "#9467bd", "#8c564b", "#e377c2",
    "#17becf", "#bcbd22", "#7f7f7f", "#393b79", "#637939",
    "#8c6d31", "#843c39", "#7b4173", "#3182bd", "#31a354",
]


def run_java_harness() -> str:
    BUILD_DIR.mkdir(parents=True, exist_ok=True)
    java_files = [str(p) for p in SOURCE_ROOT.glob("CatanAnalysis/**/*.java")]
    result = subprocess.run(["javac", "-d", str(BUILD_DIR)] + java_files, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout); print(result.stderr)
        sys.exit("Java compilation failed.")
    result = subprocess.run(["java", "-cp", str(BUILD_DIR), "CatanAnalysis.testing.SecondSettlementClustersScenario"],
                             capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout); print(result.stderr)
        sys.exit("Running SecondSettlementClustersScenario failed.")
    return result.stdout


def parse(raw_output: str):
    resources = [int(x) for x in re.search(r"RESOURCES:\s*(.+)", raw_output).group(1).split()]
    dice = [int(x) for x in re.search(r"DICE:\s*(.+)", raw_output).group(1).split()]

    strategies = []
    blocks = re.split(r"=== STRATEGY RANK (\d+) \(original index (\d+)\) ===", raw_output)[1:]
    for i in range(0, len(blocks), 3):
        rank = int(blocks[i])
        original_index = int(blocks[i + 1])
        body = blocks[i + 2]
        strategies.append({
            "rank": rank,
            "original_index": original_index,
            "strategy": re.search(r"STRATEGY:\s*(.+)", body).group(1).strip(),
            "tile_values": [float(x) for x in re.search(r"TILE_VALUES:\s*(.+)", body).group(1).split()],
            "component_ids": [int(x) for x in re.search(r"COMPONENT_IDS:\s*(.+)", body).group(1).split()],
            "opt_values": [float(x) for x in re.search(r"OPT_VALUES:\s*(.+)", body).group(1).split()],
            "best_town": int(re.search(r"BEST_TOWN:\s*(\d+)", body).group(1)),
        })
    return resources, dice, strategies


def draw_panel(ax, resources, dice, s: dict):
    tile_values = s["tile_values"]
    component_ids = s["component_ids"]
    opt_values = s["opt_values"]
    best_town = s["best_town"]
    heat_cmap = plt.cm.YlOrRd  # same warm colormap family for both hexes and towns

    for h in range(19):
        cx, cy = HEX_CENTERS[h]
        is_desert = resources[h] == 5
        face = "#e0e0e0" if is_desert else heat_cmap(tile_values[h])

        ax.add_patch(RegularPolygon((cx, cy), numVertices=6, radius=HEX_SIZE, orientation=0,
                                     facecolor=face, edgecolor="black", linewidth=1, zorder=1))

        r_name = RESOURCE_NAMES[resources[h]]
        d_num = str(dice[h]) if dice[h] != 0 else ""
        ax.text(cx, cy + 0.18, r_name, ha="center", va="center", fontsize=8, fontweight="bold", zorder=2)
        if d_num:
            ax.text(cx, cy - 0.22, d_num, ha="center", va="center", fontsize=10, fontweight="bold", zorder=2)

    # town circles, colored by this strategy's own FinalOptLocation (same colormap family
    # as the hexes above, just normalized to its own 0..max range) - no road lines at all
    vmin, vmax = min(opt_values), max(opt_values)
    for t in range(54):
        x, y = TOWN_POSITIONS[t]
        color = heat_cmap((opt_values[t] - vmin) / (vmax - vmin) if vmax > vmin else 0)
        ax.scatter(x, y, s=200, color=color, edgecolors="black", linewidths=0.7, zorder=4)
        ax.text(x, y, str(t), ha="center", va="center", fontsize=5.5, zorder=5)

    bx, by = TOWN_POSITIONS[best_town]
    ax.scatter(bx, by, s=560, facecolors="none", edgecolors="gold", linewidths=3, zorder=6)
    ax.scatter(bx, by, s=40, marker="*", color="gold", edgecolors="black", linewidths=0.6, zorder=7)

    n_clusters = len(set(component_ids[h] for h in range(19) if resources[h] != 5))
    ax.set_title(f"{s['strategy']}\n({n_clusters} connected components, best town = {best_town})",
                 fontsize=11, fontweight="bold", pad=10)

    ax.set_xlim(-5.2, 5.2)
    ax.set_ylim(-7.3, 1.5)
    ax.set_aspect("equal")
    ax.axis("off")


def main():
    print("Compiling & running SecondSettlementClustersScenario...")
    raw_output = run_java_harness()
    resources, dice, strategies = parse(raw_output)

    for s in strategies:
        print(f"Rank {s['rank']}: {s['strategy']} - best town {s['best_town']}")

    fig, axes = plt.subplots(1, 3, figsize=(26, 11))
    for ax, s in zip(axes, strategies):
        draw_panel(ax, resources, dice, s)

    fig.suptitle(
        "Finding the second settlement: the same map, 3 different strategies\n"
        "hex fill = tile ranking heatmap   |   town fill = FinalOptLocation (same color family)   |   gold = best town",
        fontsize=13.5, fontweight="bold"
    )
    fig.tight_layout(rect=[0, 0, 1, 0.90])
    fig.subplots_adjust(wspace=0.25)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    out_path = OUTPUT_DIR / "second_settlement_clusters.png"
    fig.savefig(out_path, dpi=150)
    plt.close(fig)
    print(f"Saved {out_path}")


if __name__ == "__main__":
    main()
