package taxi;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="CallList_table")
public class CallList {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Integer cost;
        private String status;
        private Long customerId;
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
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
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
