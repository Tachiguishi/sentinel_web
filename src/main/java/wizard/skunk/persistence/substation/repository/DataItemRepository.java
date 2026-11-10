package wizard.skunk.persistence.substation.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wizard.skunk.persistence.substation.domain.DataItem;
import wizard.skunk.persistence.substation.domain.DataSet;

import java.util.List;

@Repository
public interface DataItemRepository extends CrudRepository<DataItem, Long> {
	void deleteByDataSetIn(List<DataSet> dataSets);
	List<DataItem> findByDataSetOrderByFcdaIndex(DataSet dataSet);
}
