# Breaking Catan: An Algorithmic Solver

This project implements a mathematical and graph-theory approach to solving the initial placement problem in *The Settlers of Catan*. Unlike traditional solvers that rely on Deep Reinforcement Learning or extensive Brute Force simulations, this algorithm utilizes heuristic analysis, bipartite mapping, and probability theory to determine the optimal game state in **deterministic constant time**.

## Overview

The "Breaking Catan" algorithm converts the board game into a multi-layered graph model to solve complex resource management problems. The primary objective is to identify the most strategic locations for placing initial settlements based on pre-defined win conditions, resource scarcity, and map layout.

The system avoids iterative simulations. Instead, it calculates a deterministic score for every possible coordinate on the board using a custom-weighted heuristic formula that accounts for the "Law of Diminishing Returns".

## Key Features

* **Constant Time Execution:** The algorithm is architected to provide immediate analytical results without the computational overhead of recursive lookaheads.
* **Dual-Graph Representation:**
    * **Tile Graph:** Represents the 19 resource hexes using an Adjacency Matrix to identify resource clusters.
    * **Bipartite Town-Hex Graph:** Maps the relationship between the 54 settlements (Set U) and 19 hexes (Set V) to prevent illegal graph traversals.
* **Strategy Heatmaps:** Generates "heatmaps" based on a "Relative Token Score," calculating the statistical value of specific board areas relative to a chosen win condition.
* **Connected Components Analysis:** Utilizes Depth-First Search (DFS) to rank resource clusters by size, ensuring selected spots have viable pathways for future expansion.
* **Economic Heuristics:** Implements a square-root smoothing function to prioritize resource diversity over simple abundance, mitigating the risk of "flooding" a player's hand with a single resource type.

## Methodology

The solution pipeline consists of five distinct analytical stages:

### 1. Win Condition & Resource Weighting
For every candidate strategy *i* (e.g. "4 Cities + Longest Road"), each of the five resources *j* is weighted by how much the strategy needs it relative to how scarce it is on a standard board. First, each resource's natural **area probability** is its share of the 18 non-desert tiles:

$$
AreaProbability_j = \frac{Tiles_j}{18}
$$

The strategy's raw resource requirement is then inflated by scarcity into a **Weighted Need**, and normalized into a **Resource Importance** percentage (how much of the strategy's total weighted need this one resource represents):

$$
WeightedNeed_{i,j} = \frac{ResourceNeed_{i,j}}{AreaProbability_j}
\qquad\qquad
ResourceImportance_{i,j} = \frac{WeightedNeed_{i,j}}{\displaystyle\sum_{k=1}^{5} WeightedNeed_{i,k}}
$$

### 2. Probability Scoring (Relative Token Score)
`ResourceImportance` is converted into an **ideal dice pip count**. Since 2d6 sums are symmetric around 7 (a 6 and an 8 are equally likely), the engine derives one ideal number below 7 and its mirror above 7:

$$
RelativeToken^{low}_{i,j} = 6 - \frac{100\cdot\big(HighestIndex_i - ResourceImportance_{i,j}\big)}{3}
\qquad\qquad
RelativeToken^{high}_{i,j} = 7 + \left|\,RelativeToken^{low}_{i,j} - 7\,\right|
$$

where $HighestIndex_i = \max_j ResourceImportance_{i,j}$. Every tile is then graded by how close its *actual* dice number is to whichever of the two ideal numbers is nearest, using a stepped closeness function:

$$
WinIndex(tile) = ClosenessScore\Big(\min\big(|d - RelativeToken^{low}|,\; |d - RelativeToken^{high}|\big)\Big),\quad d = DiceNumber(tile)
$$

$$
ClosenessScore(0)=1.0,\;\; ClosenessScore(1)=0.75,\;\; ClosenessScore(2)=0.5,\;\; ClosenessScore(3)=0.25,\;\; ClosenessScore(\geq4)=0
$$

This gives every tile a heatmap value in $[0,1]$ for the given strategy. It's the "pre-town" ranking used to color the board and to group tiles into **Connected Components** (tiles of equal `WinIndex` that touch each other, found via DFS and ranked by size) before any settlement is scored.

### 3. Bipartite Graph Construction
To score a settlement without illegally "hopping" between non-adjacent nodes, the system builds a **Bipartite Graph** between the 54 settlement spots (Set U) and the 19 resource hexes (Set V). Each settlement is linked only to the 1, 2, or 3 hexes it physically touches, so its score can be accumulated strictly from its own neighborhood.

### 4. The Optimization Formula (Tuning)
For a settlement touching a hex from a connected component of size $|Component|$, the component's relative footprint on the map is:

$$
ClusterSizeFactor = \frac{|Component|}{19}
$$

Each of the five resources contributes a per-resource score, weighted by the strategy's need for it ($ResourceImportance$), the tile's dice quality ($WinIndex$), and the size of the resource cluster it belongs to:

$$
OptLocation_{array}[k] = ResourceImportance_{i,k} \cdot WinIndex^{\beta} \cdot ClusterSizeFactor^{\gamma}
$$

with tuning exponents $\beta = 1.5$ (win-index emphasis) and $\gamma = 0.5$ (cluster-size emphasis). To simulate **Diminishing Marginal Utility**, so that stacking five mediocre resources doesn't outscore two excellent, diverse ones, each resource's contribution is square-root-smoothed before being summed, and accumulated additively across every hex the settlement touches:

$$
FinalOptLocation(town) = \sum_{k=1}^{5} \sqrt{OptLocation_{array}[k]}
$$

### 5. Road Placement (Danger/Opportunity Graph & Dijkstra)
Once the optimal settlement is chosen, the engine picks a direction for the adjacent road. This is framed as a variant of the **Canadian Traveler Problem (CTP)**: the board is dynamic, and an opponent may block a promising route before it's completed, so a naive "shortest path to the best tile" heuristic is unsafe.

Instead, the algorithm builds a weighted **danger/opportunity graph** over the road network and runs Dijkstra's algorithm from the newly placed settlement. Each edge $(u,v)$ is scored as:

$$
W(u,v) = \max\Big(\epsilon,\; BaseCost + Danger(v) - AdjustedOpt(v)\Big)
$$

**Danger** penalizes edges near an opponent, scaled by a risk-aversion coefficient $\lambda$ that isn't picked arbitrarily but derived from an **indifference-point** argument (game theory): the point at which a risky-but-excellent road and a safe-but-average one become equally attractive.

$$
Danger(v) = \frac{MaxOpt \cdot \lambda}{d_{opp}(v)^2}
\qquad\qquad
\lambda = d_{ref}^{\,2}\cdot\left(1 - \frac{u}{MaxOpt}\right)
$$

where $MaxOpt$ is the best `FinalOptLocation` still available on the board, $d_{opp}(v)$ is the distance from $v$ to the nearest opponent, $u$ is the average `FinalOptLocation` of the player's "safe" nodes, and $d_{ref}$ is the opponent's distance to the $MaxOpt$ node.

**AdjustedOpt** discounts a destination's score by how far it is from the player, via a decay factor $\gamma$ (unrelated to the $\gamma$ in §4) that is itself bounded by $D_{max}$, the reach of the player's own **safe space**: the set of nodes strictly closer to the player than to any opponent, found via a multi-source BFS equivalent to a discrete **Voronoi partition** of the board.

$$
AdjustedOpt(v) = OptLocation(v)\cdot\gamma^{\,d_{us}(v)}
\qquad\qquad
\gamma = \sqrt[D_{max}]{\dfrac{\mu}{MaxOpt}}
\qquad\qquad
D_{max} = \max_{v\,\in\,SafeNodes} d_{us}(v)
$$

Finally, $\epsilon$ is a small positive floor that keeps every edge weight non-negative, since classic Dijkstra breaks down with negative weights. The **first edge** of the resulting shortest path, the immediate direction out of the settlement, is returned as the optimal road placement.

### 5. Road Placement (Danger/Opportunity Graph & Dijkstra)
Once the optimal settlement is chosen, the engine must pick a direction for the adjacent road. This is framed as a variant of the **Canadian Traveler Problem (CTP)**: the board is dynamic, and an opponent may block a promising route before it can be completed, so a purely greedy "shortest path to the best tile" heuristic is unsafe.

Instead of weighting every edge equally, the algorithm builds a weighted **danger/opportunity graph** over the road network and runs Dijkstra's algorithm from the newly placed settlement. Each edge (u, v) is scored as:

$$
W(u,v) = \max\Big(\epsilon,\; BaseCost + Danger(v) - AdjustedOpt(v)\Big)
$$

* **`BaseCost`** — a fixed per-road cost, so Dijkstra naturally favors shorter routes unless a detour pays for itself.
* **`Danger(v)`** — a penalty proportional to how close the nearest opponent is to node *v*, scaled by a risk-aversion coefficient **λ**. Rather than picking λ arbitrarily, it's derived from an **indifference-point** argument (game theory): the point where a risky-but-excellent road and a safe-but-average one are equally attractive.
* **`AdjustedOpt(v)`** — the destination's `FinalOptLocation` score, discounted by a distance-decay factor **γ** so far-away tiles are worth less. γ itself is bounded by **`Dmax`**, the reach of the player's own "safe space" — the set of nodes closer to the player than to any opponent, computed via a multi-source BFS equivalent to a discrete **Voronoi partition** of the board.
* **`ε`** — a small positive floor that keeps every edge weight non-negative, since classic Dijkstra breaks down with negative weights.

The first edge of the resulting shortest path — i.e. the immediate direction out of the settlement — is returned as the optimal road placement.

## Technical Implementation

* **Core Logic:** Java
* **Visualization:** Python (Heatmap generation)
* **Data Structures:**
    * `int[][] adjacencyMatrix` for Tile-to-Tile connections.
    * `Town` objects with direct reference lists for Bipartite (Town-to-Hex) connections.
    * Linked Lists for sorting Connected Components.

## Customizable Strategies

The algorithm is designed to be strategy-agnostic. Users can input a custom list of required resources to define a specific strategy. For demonstration, the system currently visualizes:

1.  **4 Cities + Longest Road**
2.  **2 Cities + 1 Settlement + Longest Road + Largest Army**
3.  **3 Cities + 2 Settlements + Longest Road**
4.  **2 Cities + 3 Settlements + Largest Army**
5.  **1 City + 3 Settlements + Longest Road + Largest Army**

## Credits

* **Author:** Gal Reuveni
* **Original Research:** *Breaking Catan*
* **Publication:** *Digital Whisper*, Issue 2026
