# Equiscope Features

## Interactive Weight Adjustment System

### Left Sidebar Controls
- **Real-time Sliders**: Adjust the four key resilience metrics independently:
  - Income Weight (default: 50%)
  - Unemployment Weight (default: 25%)
  - Cost of Living Weight (default: 15%)
  - Disaster Risk Weight (default: 10%)

- **Total Weight Validation**: 
  - Green indicator when weights sum to 100%
  - Red indicator when weights don't sum to 100%
  
- **Reset Button**: Instantly restore default weights

### Live Updates
All visualizations update in real-time as you adjust the sliders:
- North Carolina county map re-colors based on new scores
- Bar chart re-sorts and re-colors counties
- County details table recalculates all scores

## North Carolina Resilience Map

### Interactive Choropleth Map
- **Color-Coded Counties**: 
  - 🔴 Red (0.0-0.5): Low resilience
  - 🟠 Orange (0.5-0.7): Medium resilience  
  - 🟢 Green (0.7-1.0): High resilience
  - 🔵 Cyan (highest): Top resilience scores

- **Hover Information**: 
  - County name
  - Exact resilience score (3 decimal places)

- **Geographic Focus**: 
  - Centered on North Carolina
  - County boundaries from NCDOT GeoJSON API
  - Dark theme matching overall dashboard

## Data Sources

### NCDOT County Boundaries
- **Source**: NC Department of Transportation GIS Services
- **API**: https://gis11.services.ncdot.gov/arcgis/rest/services/NCDOT_CountyBdy_Poly/MapServer/0/query?outFields=*&where=1%3D1&f=geojson
- **Format**: GeoJSON with county polygons
- **Updates**: Real-time from official NCDOT service

### Real Population Data (Fixed)
- Previously: All counties hardcoded to 50,000 population
- Now: Real 2022 Census data via ACS API variable B01003_001E
- Population penalties applied:
  - <2,000 residents: -8% score penalty
  - <10,000 residents: -5% score penalty

### Census Bureau ACS 2022
- Median household income (B19013_001E) - normalized 0-1 scale
- Population counts (B01003_001E)
- 100 North Carolina counties loaded on startup

## UI/UX Improvements

### Layout
- **Flexbox Design**: Sidebar + main content area
- **Sticky Sidebar**: Always visible controls during scroll
- **Responsive**: Adapts to different screen sizes

### Dark Theme
- Background: Pure black (#000)
- Primary accent: Cyan (#00d4ff)
- Success green: #00ff88
- Warning orange: #ffa500
- Error red: #ff4444

### Custom Scrollbars
- Cyan-themed scrollbar in sidebar and county table
- Smooth hover transitions on sliders

## Technical Stack

- **Frontend**: Vanilla JavaScript, Plotly.js for visualizations
- **Backend**: Spring Boot 3.2.0, Java 21
- **AI Integration**: AWS Bedrock (Titan Text Express)
- **Data**: Census Bureau API, NCDOT GeoJSON API
- **Build**: Maven 3.9.11

## Access

🌐 **Local URL**: http://localhost:8080

Server runs on port 8080 with AWS credentials configured via environment variables.
