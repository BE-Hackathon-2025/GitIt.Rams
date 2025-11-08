package com.example.resilience.model;

public class County {
    private Long id;
    private String name;
    private int population;
    private double medianIncome; // normalized 0-1 or raw dollars
    private double unemploymentRate; // 0-1
    private double costOfLivingIndex; // 0-1
    private double disasterRisk; // 0-1 probability estimate

    public County() {}

    public County(Long id, String name, int population, double medianIncome, double unemploymentRate, double costOfLivingIndex, double disasterRisk) {
        this.id = id;
        this.name = name;
        this.population = population;
        this.medianIncome = medianIncome;
        this.unemploymentRate = unemploymentRate;
        this.costOfLivingIndex = costOfLivingIndex;
        this.disasterRisk = disasterRisk;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; }
    public double getMedianIncome() { return medianIncome; }
    public void setMedianIncome(double medianIncome) { this.medianIncome = medianIncome; }
    public double getUnemploymentRate() { return unemploymentRate; }
    public void setUnemploymentRate(double unemploymentRate) { this.unemploymentRate = unemploymentRate; }
    public double getCostOfLivingIndex() { return costOfLivingIndex; }
    public void setCostOfLivingIndex(double costOfLivingIndex) { this.costOfLivingIndex = costOfLivingIndex; }
    public double getDisasterRisk() { return disasterRisk; }
    public void setDisasterRisk(double disasterRisk) { this.disasterRisk = disasterRisk; }
}
