package taxi;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CallListRepository extends CrudRepository<CallList, Long> {


}