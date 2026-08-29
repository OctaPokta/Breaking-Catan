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

The solution pipeline consists of several distinct analytical stages:

### 1. Win Condition & Resource Weighting
The engine evaluates distinct strategies (e.g., "4 Cities" vs. "Road Building") and assigns weighted importance to the five game resources based on the total cost to achieve victory. It calculates a **Weighted Need** by comparing these requirements against the map's natural resource distribution.

### 2. Probability Scoring (Relative Token Score)
The system cross-references the dice number tokens with resource importance. It calculates a "Relative Token Score" for each tile using the standard 2D6 normal distribution, applying specific error ranges (Exact, ±1, ±2) to grade the quality of every tile.

### 3. Bipartite Graph Construction
To accurately score a settlement without "hopping" illegally between nodes, the system constructs a **Bipartite Graph**. This structure separates Settlement Nodes from Resource Nodes, allowing the algorithm to sum the value of adjacent resources (1, 2, or 3 hexes) directly into the settlement's score object.

### 4. The Optimization Formula (Tuning)
The final score for any given settlement is derived from a tuned heuristic formula. It balances three factors: **Resource Weight** (strategy need), **Win Index** (dice probability), and **Cluster Size** (expansion potential).

To simulate the economic concept of Diminishing Marginal Utility, the final score allows for "punishment" of redundant resources using a square root function:

$$
FinalOptLocation = \sum_{i=1}^{5} \sqrt{OptLocation_{array}[i]}
$$

*Note: The algorithm applies tuning parameters (&alpha;, &beta;, &gamma;) to weight the importance of the Win Index versus the Cluster Size depending on the selected strategy.*

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
