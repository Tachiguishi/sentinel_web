package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.DataItem;
import wizard.skunk.persistence.substation.domain.DataSet;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.FCDA;
import wizard.skunk.persistence.substation.repository.DataItemRepository;
import wizard.skunk.persistence.substation.repository.FCDARepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class FCDAService {
	private final FCDARepository fcdaRepository;
	private final DataItemRepository dataItemRepository;

	@Autowired
	public FCDAService(FCDARepository repository, DataItemRepository dataItemRepository){
		this.fcdaRepository = repository;
		this.dataItemRepository = dataItemRepository;
	}

	public List<FCDA> find(){
		List<FCDA> result = (List<FCDA>) fcdaRepository.findAll();
		result.removeIf(fcda -> fcda.getDevice() == null || fcda.getDevice().isIgnored());
		return result;
	}

	public List<FCDA> findByDevice(Device device){
		return (List<FCDA>) fcdaRepository.findByDevice(device);
	}

	public List<FCDA> findByDataSet(DataSet dataSet){
		List<FCDA> result = new ArrayList<>();
		List<DataItem> dataItems = dataItemRepository.findByDataSetOrderByFcdaIndex(dataSet);
		for(DataItem dataItem : dataItems){
			result.add(dataItem.getFcda());
		}
		return result;
	}

	public List<FCDA> findByDotReferenceLike(String dotReferencePattern){
		return (List<FCDA>) fcdaRepository.findByDotReferenceContains(dotReferencePattern);
	}

	public FCDA findByDeviceAndDotReference(Device device, String dotReference){
		return fcdaRepository.findTopByDeviceAndDotReference(device, dotReference);
	}

	public List<FCDA> findAllByDeviceAndDotReference(Device device, String dotReferencePrefix){
		return (List<FCDA>) fcdaRepository.findByDeviceAndDotReference(device, dotReferencePrefix);
	}

	@Transactional
	public void delete(FCDA fcda){
		fcdaRepository.delete(fcda);
	}

	@Transactional
	public void deleteByDevice(Device device) {
		fcdaRepository.deleteAll(fcdaRepository.findByDevice(device));
	}

	public void saveAll(List<FCDA> fcdaList){
		fcdaRepository.saveAll(fcdaList);
	}
}
