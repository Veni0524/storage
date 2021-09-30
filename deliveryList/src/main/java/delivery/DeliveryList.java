package delivery;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="DeliveryList_table")
public class DeliveryList {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Integer cost;
        private Long customerId;
        private String status;
        private Integer settled;
        private String destination;


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
        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public Integer getSettled() {
            return settled;
        }

        public void setSettled(Integer settled) {
            this.settled = settled;
        }
        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

}