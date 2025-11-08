let data = [];
const stateSelect = document.getElementById("stateSelect");
const stateInfo = document.getElementById("stateInfo");

const stateAbbr = {
  "Alabama": "AL", "Alaska": "AK", "Arizona": "AZ", "Arkansas": "AR", "California": "CA",
  "Colorado": "CO", "Connecticut": "CT", "Delaware": "DE", "Florida": "FL", "Georgia": "GA",
  "Hawaii": "HI", "Idaho": "ID", "Illinois": "IL", "Indiana": "IN", "Iowa": "IA",
  "Kansas": "KS", "Kentucky": "KY", "Louisiana": "LA", "Maine": "ME", "Maryland": "MD",
  "Massachusetts": "MA", "Michigan": "MI", "Minnesota": "MN", "Mississippi": "MS", "Missouri": "MO",
  "Montana": "MT", "Nebraska": "NE", "Nevada": "NV", "New Hampshire": "NH", "New Jersey": "NJ",
  "New Mexico": "NM", "New York": "NY", "North Carolina": "NC", "North Dakota": "ND", "Ohio": "OH",
  "Oklahoma": "OK", "Oregon": "OR", "Pennsylvania": "PA", "Rhode Island": "RI", "South Carolina": "SC",
  "South Dakota": "SD", "Tennessee": "TN", "Texas": "TX", "Utah": "UT", "Vermont": "VT",
  "Virginia": "VA", "Washington": "WA", "West Virginia": "WV", "Wisconsin": "WI", "Wyoming": "WY"
};

function normalizeWeights(w1, w2, w3) {
  const total = w1 + w2 + w3;
  return [w1 / total, w2 / total, w3 / total];
}

function calculateScores() {
  const wIncome = parseFloat(document.getElementById("income").value);
  const wUnemp = parseFloat(document.getElementById("unemployment").value);
  const wCost = parseFloat(document.getElementById("cost").value);
  const [wi, wu, wc] = normalizeWeights(wIncome, wUnemp, wCost);

  data.forEach(d => {
    d.Resilience_Score = +(wi * d.Income_Norm + wu * (1 - d.Unemployment_Norm) + wc * (1 - d.Cost_Norm)).toFixed(3);
  });

  updateStateDropdown();
  updateCharts();
}

function updateStateDropdown() {
  stateSelect.innerHTML = "";
  data.sort((a, b) => a.State.localeCompare(b.State)).forEach(d => {
    const option = document.createElement("option");
    option.value = d.State;
    option.textContent = d.State;
    stateSelect.appendChild(option);
  });
  updateStateInfo();
}

function updateStateInfo() {
  const selected = stateSelect.value;
  const state = data.find(d => d.State === selected);
  const rank = [...data].sort((a, b) => b.Resilience_Score - a.Resilience_Score)
    .findIndex(d => d.State === selected) + 1;
  stateInfo.textContent = `${selected} Score: ${state.Resilience_Score} (Rank #${rank})`;
}

function updateCharts() {
  const sorted = [...data].sort((a, b) => b.Resilience_Score - a.Resilience_Score);
  Plotly.newPlot("barChart", [{
    x: sorted.map(d => d.State),
    y: sorted.map(d => d.Resilience_Score),
    type: "bar"
  }], {
    title: "Resilience Score by State"
  });

  Plotly.newPlot("mapChart", [{
    type: "choropleth",
    locationmode: "USA-states",
    locations: sorted.map(d => stateAbbr[d.State]),
    z: sorted.map(d => d.Resilience_Score),
    colorscale: "Viridis",
    colorbar: { title: "Score" }
  }], {
    geo: { scope: "usa" },
    title: "U.S. Resilience Map"
  });
}

document.querySelectorAll("input[type=range]").forEach(el => {
  el.addEventListener("input", calculateScores);
});
stateSelect.addEventListener("change", updateStateInfo);

fetch("resilience_data.json")
  .then(res => res.json())
  .then(json => {
    data = json;
    calculateScores();
  });