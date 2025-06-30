package fraud.fraud.entitys;

public enum Threat {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    IMMEDIATE(4);

    private final int risk;

    Threat(int risk){
        this.risk = risk;
    }
    public int getRisk() {
        return risk;
    }
}
