// Global state
let countiesData = [];
let mapInstance = null;
let geoJsonLayer = null;
let weights = {
  income: 0.50,
  unemployment: 0.25,
  cost: 0.15,
  disaster: 0.10
};

// Expose weights globally for AI handler
window.weights = weights;

async function fetchCounties() {
  const res = await fetch('/api/counties');
  return res.json();
}

async function fetchScore(id) {
  const res = await fetch(`/api/score/${id}`);
  return res.json();
}

function calculateScore(county) {
  // Use custom weights for client-side calculation
  const income = county.medianIncome || 0;
  const employment = 1 - (county.unemploymentRate || 0);
  const cost = 1 - (county.costOfLivingIndex || 0);
  const disaster = 1 - (county.disasterRisk || 0);
  
  let score = (income * weights.income) + 
              (employment * weights.unemployment) + 
              (cost * weights.cost) + 
              (disaster * weights.disaster);
  
  // Population penalty
  if (county.population < 2000) {
    score *= 0.92;
  } else if (county.population < 10000) {
    score *= 0.95;
  }
  
  return Math.max(0, Math.min(1, score));
}

function updateVisualizations() {
  if (!countiesData || countiesData.length === 0) return;
  
  // Recalculate scores with current weights
  countiesData.forEach(c => {
    c.score = calculateScore(c);
  });
  
  const sorted = [...countiesData].sort((a, b) => b.score - a.score);
  
  // Update map colors if map exists
  if (geoJsonLayer) {
    updateMapColors();
  }
  
  renderChart(sorted);
  renderTable(sorted);
}

function initializeMap() {
  const mapDiv = document.getElementById('map');
  if (!mapDiv || mapInstance) return;
  
  // Initialize Leaflet map
  mapInstance = L.map('map').setView([35.5, -79.5], 7);
  
  // Add OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
  }).addTo(mapInstance);
  
  // Fetch NCDOT GeoJSON
  fetch('https://gis11.services.ncdot.gov/arcgis/rest/services/NCDOT_CountyBdy_Poly/MapServer/0/query?outFields=*&where=1%3D1&f=geojson')
    .then(res => res.json())
    .then(geojson => {
      // Create county lookup
      const countyLookup = {};
      countiesData.forEach(c => {
        const key = c.name.replace(' County', '').trim().toUpperCase();
        countyLookup[key] = c;
      });
      
      // Add GeoJSON layer
      geoJsonLayer = L.geoJSON(geojson, {
        style: function(feature) {
          return getFeatureStyle(feature, countyLookup);
        },
        onEachFeature: function(feature, layer) {
          const props = feature.properties;
          const countyName = props.CountyName || props.NAME || props.name || 'Unknown';
          const countyKey = countyName.replace(' County', '').trim().toUpperCase();
          const county = countyLookup[countyKey];
          
          if (county) {
            layer.bindPopup(`
              <strong>${countyName}</strong><br>
              Score: ${county.score.toFixed(3)}<br>
              Population: ${county.population.toLocaleString()}<br>
              Median Income: ${county.medianIncome.toFixed(3)}<br>
              Unemployment: ${county.unemploymentRate.toFixed(3)}<br>
              Cost of Living: ${county.costOfLivingIndex.toFixed(3)}<br>
              Disaster Risk: ${county.disasterRisk.toFixed(3)}
            `);
          }
        }
      }).addTo(mapInstance);
    })
    .catch(err => {
      console.error('Error loading map:', err);
      mapDiv.innerHTML = `<div style="color: #ff4444; padding: 20px;">Error loading map: ${err.message}</div>`;
    });
}

function getFeatureStyle(feature, countyLookup) {
  const props = feature.properties;
  const countyName = props.CountyName || props.NAME || props.name || '';
  const countyKey = countyName.replace(' County', '').trim().toUpperCase();
  const county = countyLookup[countyKey];
  const score = county ? county.score : 0;
  
  let fillColor;
  if (score > 0.7) fillColor = '#00ff88';
  else if (score > 0.5) fillColor = '#ffa500';
  else fillColor = '#ff4444';
  
  return {
    fillColor: fillColor,
    weight: 1,
    opacity: 1,
    color: '#00d4ff',
    fillOpacity: 0.7
  };
}

function updateMapColors() {
  if (!geoJsonLayer) return;
  
  // Create county lookup
  const countyLookup = {};
  countiesData.forEach(c => {
    const key = c.name.replace(' County', '').trim().toUpperCase();
    countyLookup[key] = c;
  });
  
  // Update each layer's style
  geoJsonLayer.eachLayer(function(layer) {
    if (layer.feature) {
      const newStyle = getFeatureStyle(layer.feature, countyLookup);
      layer.setStyle(newStyle);
      
      // Update popup
      const props = layer.feature.properties;
      const countyName = props.CountyName || props.NAME || props.name || 'Unknown';
      const countyKey = countyName.replace(' County', '').trim().toUpperCase();
      const county = countyLookup[countyKey];
      
      if (county) {
        layer.bindPopup(`
          <strong>${countyName}</strong><br>
          Score: ${county.score.toFixed(3)}<br>
          Population: ${county.population.toLocaleString()}<br>
          Median Income: ${county.medianIncome.toFixed(3)}<br>
          Unemployment: ${county.unemploymentRate.toFixed(3)}<br>
          Cost of Living: ${county.costOfLivingIndex.toFixed(3)}<br>
          Disaster Risk: ${county.disasterRisk.toFixed(3)}
        `);
      }
    }
  });
}

function renderChart(counties) {
  const names = counties.map(c => c.name);
  const scores = counties.map(c => c.score);
  const colors = scores.map(s => {
    if (s > 0.7) return '#00ff88';
    if (s > 0.5) return '#ffa500';
    return '#ff4444';
  });

  const data = [{
    x: names,
    y: scores,
    type: 'bar',
    marker: { color: colors }
  }];

  const layout = {
    title: { text: 'County Resilience Scores', font: { color: '#00d4ff', size: 18 } },
    paper_bgcolor: '#111',
    plot_bgcolor: '#1a1a1a',
    font: { color: '#e0e0e0' },
    xaxis: { 
      title: 'County',
      tickangle: -45,
      tickfont: { size: 8 },
      color: '#b0b0b0'
    },
    yaxis: { 
      title: 'Score',
      range: [0, 1],
      color: '#b0b0b0'
    },
    margin: { b: 120, l: 60, r: 30, t: 60 }
  };

  Plotly.newPlot('chart', data, layout, { responsive: true });
}

function renderTable(counties) {
  const tbody = document.querySelector('#county-table tbody');
  tbody.innerHTML = '';
  counties.forEach(c => {
    const tr = document.createElement('tr');
    const population = (c.population || 0).toLocaleString();
    const income = (c.medianIncome || 0).toFixed(3);
    const unemployment = (c.unemploymentRate || 0).toFixed(3);
    const cost = (c.costOfLivingIndex || 0).toFixed(3);
    const disaster = (c.disasterRisk || 0).toFixed(3);
    const score = (c.score || 0).toFixed(3);
    const scoreColor = c.score > 0.7 ? '#00ff88' : c.score > 0.5 ? '#ffa500' : '#ff4444';
    
    tr.innerHTML = `
      <td>${c.name || 'Unknown'}</td>
      <td>${population}</td>
      <td>${income}</td>
      <td>${unemployment}</td>
      <td>${cost}</td>
      <td>${disaster}</td>
      <td style="font-weight: bold; color: ${scoreColor}">${score}</td>
    `;
    tbody.appendChild(tr);
  });
}

// Slider event handlers
function setupSliders() {
  const sliders = {
    income: document.getElementById('income-slider'),
    unemployment: document.getElementById('unemployment-slider'),
    cost: document.getElementById('cost-slider'),
    disaster: document.getElementById('disaster-slider')
  };
  
  const valueDisplays = {
    income: document.getElementById('income-value'),
    unemployment: document.getElementById('unemployment-value'),
    cost: document.getElementById('cost-value'),
    disaster: document.getElementById('disaster-value')
  };
  
  const totalDisplay = document.getElementById('total-value');
  const totalContainer = document.getElementById('total-weight');
  
  function updateWeights() {
    weights.income = parseInt(sliders.income.value) / 100;
    weights.unemployment = parseInt(sliders.unemployment.value) / 100;
    weights.cost = parseInt(sliders.cost.value) / 100;
    weights.disaster = parseInt(sliders.disaster.value) / 100;
    
    // Update global reference
    window.weights = weights;
    
    valueDisplays.income.textContent = sliders.income.value + '%';
    valueDisplays.unemployment.textContent = sliders.unemployment.value + '%';
    valueDisplays.cost.textContent = sliders.cost.value + '%';
    valueDisplays.disaster.textContent = sliders.disaster.value + '%';
    
    const total = parseInt(sliders.income.value) + parseInt(sliders.unemployment.value) + 
                  parseInt(sliders.cost.value) + parseInt(sliders.disaster.value);
    totalDisplay.textContent = total + '%';
    
    if (total === 100) {
      totalContainer.classList.add('valid');
      totalContainer.classList.remove('invalid');
    } else {
      totalContainer.classList.add('invalid');
      totalContainer.classList.remove('valid');
    }
    
    // Update visualizations immediately
    if (countiesData && countiesData.length > 0) {
      updateVisualizations();
    }
  }
  
  Object.values(sliders).forEach(slider => {
    slider.addEventListener('input', updateWeights);
  });
  
  // Reset button
  document.getElementById('reset-weights').addEventListener('click', () => {
    sliders.income.value = 50;
    sliders.unemployment.value = 25;
    sliders.cost.value = 15;
    sliders.disaster.value = 10;
    updateWeights();
  });
}

async function loadAndRender() {
  try {
    countiesData = await fetchCounties();
    console.log('Loaded counties:', countiesData.length);
    
    if (!countiesData || countiesData.length === 0) {
      throw new Error('No county data received from API');
    }
    
    // Calculate initial scores
    countiesData.forEach(c => {
      c.score = calculateScore(c);
    });
    
    // Initialize map
    initializeMap();
    
    updateVisualizations();
  } catch (err) {
    console.error('Error loading dashboard:', err);
    document.getElementById('chart').innerHTML = '<div style="color: #ff4444; padding: 20px;">Error loading data: ' + err.message + '</div>';
  }
}

setupSliders();
loadAndRender();
