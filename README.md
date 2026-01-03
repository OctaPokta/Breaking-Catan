# Breaking Catan: An Algorithmic Solver

This project implements a mathematical and graph-theory approach to solving the initial placement problem in *The Settlers of Catan*. Unlike traditional solvers that rely on Deep Reinforcement Learning or extensive Brute Force simulations, this algorithm utilizes heuristic analysis and probability theory to determine the optimal game state in $O(1)$ time.

## Overview

The "Breaking Catan" algorithm converts the board game into a graph model to solve complex resource management problems. The primary objective is to identify the most strategic locations for placing initial settlements based on pre-defined win conditions, resource scarcity, and map layout.

The system avoids iterative simulations, instead calculating a deterministic score for every possible coordinate on the board using a custom-weighted heuristic formula.

## Key Features

* **Constant Time Complexity:** The algorithm is architected to run in $O(1)$ time, providing immediate analytical results without computational overhead.
* **Graph-Based Map Representation:** The hexagonal map is transformed into a graph structure where tiles act as nodes and connections are defined via an Adjacency Matrix.
* **Strategy Heatmaps:** The system generates "heatmaps" for selected winning strategies, calculating the relative importance of specific board areas based on required resource distributions.
* **Expansion Logic:** Utilizes Depth-First Search (DFS) to analyze "Connected Components," ensuring that selected settlement spots have viable pathways for future road and settlement expansion.
* **Economic Heuristics:** Implements the economic principle of "Diminishing Returns" (via square root smoothing) to prioritize resource diversity over simple abundance.

## Methodology

The solution pipeline consists of several distinct analytical stages:

### 1. Win Condition Analysis
The engine evaluates distinct strategies and assigns weighted importance to the five game resources (Brick, Wood, Ore, Wheat, Sheep) based on the total cost to achieve victory.

### 2. Probability Scoring
The system cross-references the dice number tokens on the board with the resource distribution. It calculates a "Relative Token Score" for each tile, accounting for the statistical probability of dice rolls (normal distribution of 2D6).

### 3. Bipartite Graph Construction
To map the relationship between settlement locations (intersections) and resource tiles (hexes), a Bipartite Graph is constructed. This separates the graph into two distinct sets of vertices, allowing the algorithm to analyze the adjacency between specific settlements and their surrounding resources.

### 4. Optimization Formula
The final score for any given settlement location is derived from a tuned formula that balances resource weight, strategic importance (Win Index), and cluster size. To prevent the algorithm from selecting a location with high quantity but low diversity (e.g., three wood tiles), it applies a square root function to simulate diminishing marginal utility.

$$FinalOptLocation = \sum_{i=1}^{5} \sqrt{OptLocation_{array}[i]}$$

## Technical Implementation

* **Core Logic:** Java.
* **Visualization:** Python.
* **Data Structures:** Adjacency Matrices, Linked Lists, and custom Graph Nodes.

## Customizable Strategies

The algorithm is designed to be strategy-agnostic. While specific examples are provided, the system accepts any user-defined win condition. Users can input a custom list of required resources (e.g., total Brick, Wood, Ore needed) to define a specific strategy.

For demonstration purposes, the documentation visualizes the following example strategies:
1.  **4 Cities + Longest Road**
2.  **2 Cities + 1 Settlement + Longest Road + Largest Army**
3.  **3 Cities + 2 Settlements + Longest Road**
4.  **2 Cities + 3 Settlements + Largest Army**
5.  **1 City + 3 Settlements + Longest Road + Largest Army**

## Credits

* **Author:** Gal Reuveni.
* **Publication:** *Digital Whisper*, Issue 2026.

---
*Disclaimer: This project simulates optimal moves based on statistical probability and graph theory. Actual gameplay outcomes may vary based on opponent behavior and trading dynamics.*
