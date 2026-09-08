# Graphiti upstream pin

Repository: https://github.com/getzep/graphiti
Pinned commit: `a6e026f8fca27af9c1fc4cfe359c3834dc9655bb`
License: Apache-2.0

LifeOS use:
- temporal entity/relationship graph
- fact validity windows and supersession history
- episode provenance
- hybrid graph retrieval
- Social Radar and Decision Memory foundation

Integration rule: Graphiti intelligence stays upstream. Android code under `com.kareem.lifeos.graphiti` is transport/UI glue only and must not duplicate Graphiti graph logic.

Upstream REST service: `server/graph_service`.
Verified endpoints used by LifeOS: `GET /healthcheck`, `POST /search`.
