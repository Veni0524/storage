package taxi.external;

public class Payment {

    private Long id;
    private Long callId;
    private Integer cost;
    private String destination;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getCallId() {
        return callId;
    }
    public void setCallId(Long callId) {
        this.callId = callId;
    }
    public Integer getCost() {
        return cost;
    }
    public void setCost(Integer cost) {
        this.cost = cost;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }

}
