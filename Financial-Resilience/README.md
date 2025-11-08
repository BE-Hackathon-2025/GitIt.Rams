# Financial Resilience — Spring Boot Dashboard (NC counties)

This project has been updated to a minimal Spring Boot web application that exposes a mock API and a static web dashboard.

What it does
### Features
- Interactive resilience scoring (income, unemployment, cost of living, disaster risk)
- Single authoritative North Carolina county choropleth map (enriched GeoJSON from backend)
- AWS Bedrock Titan Text Express integration for AI insights
### Architecture
The frontend fetches counties and a single enriched GeoJSON (`/api/resilience-geojson`) that merges NCDOT polygons with Census metrics and computed resilience score; only the map is displayed.
### Endpoints
- `/api/counties` - county metric data
- `/api/score/{id}` - individual score
- `/api/resilience-geojson` - enriched merged GeoJSON (authoritative map source)
- `/api/ncdot-geojson` - deprecated (returns 410)
2. From the project folder `Financial-Resilience` run:

```powershell
# build
mvn -U clean package

# run
mvn spring-boot:run
```

3. Open http://localhost:8080/ in your browser. The dashboard will load and call the API endpoints.

API endpoints
- GET /api/counties — list of counties
- GET /api/counties/{id} — county details
- GET /api/score/{id} — compute a resilience score and explanation for the county
- POST /api/score — compute score for a posted County JSON payload

Notes / Next steps
- The AI scoring is a placeholder heuristic. I can replace it with a real ML model or an external LLM call (OpenAI, Azure, etc.) if you provide API keys and desired prompt/contract.
- I seeded a small set of NC counties in `DataLoader`. We can replace that with a database or a real data ingestion pipeline.
- The frontend is intentionally simple (no build step). If you prefer React/Vite/TypeScript, I can scaffold that.

If you want me to:
- Replace the seeded dataset with a real dataset for all NC counties, I can add a CSV importer or wire a mock API to return realistic values.
- Integrate a real AI model, tell me which provider and I'll add a pluggable service layer and environment configuration.

