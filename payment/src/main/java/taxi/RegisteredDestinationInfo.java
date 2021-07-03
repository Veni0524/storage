package taxi;

public class RegisteredDestinationInfo extends AbstractEvent {

    private Long id;
    private Integer cost;
    private String destination;

    public RegisteredDestinationInfo(){
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
