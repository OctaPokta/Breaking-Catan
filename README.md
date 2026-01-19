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
