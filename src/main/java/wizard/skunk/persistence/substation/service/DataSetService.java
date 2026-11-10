package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.*;
import wizard.skunk.persistence.substation.repository.DataItemRepository;
import wizard.skunk.persistence.substation.repository.DataSetRepository;
import wizard.skunk.persistence.substation.repository.GooseSvControlRepository;
import wizard.skunk.persistence.substation.repository.ReportControlRepository;

import java.util.List;

@Service
public class DataSetService {
	private final DataSetRepository dataSetRepository;
	private final DataItemRepository dataItemRepository;
	private final ReportControlRepository reportControlRepository;
	private final GooseSvControlRepository gooseSvControlRepository;

	@Autowired
	public DataSetService(DataSetRepository dataSetRepository,
	                      DataItemRepository dataItemRepository,
	                      ReportControlRepository reportControlRepository,
	                      GooseSvControlRepository gooseSvControlRepository){
		this.dataSetRepository = dataSetRepository;
		this.dataItemRepository = dataItemRepository;
		this.reportControlRepository = reportControlRepository;
		this.gooseSvControlRepository = gooseSvControlRepository;
	}

	public List<DataSet> find(){
		List<DataSet> result = (List<DataSet>) dataSetRepository.findAll();
		result.removeIf(dataSet -> dataSet.getDevice() == null || dataSet.getDevice().isIgnored());
		return result;
	}

	public List<DataSet> findByDevice(Device device){
		return (List<DataSet>) dataSetRepository.findByDevice(device);
	}

	public List<ReportControl> findReportControlByDevice(Device device){
		return (List<ReportControl>) reportControlRepository.findByDevice(device);
	}

	public List<GooseSvControl> findGooseSvControlByDevice(Device device){
		return (List<GooseSvControl>) gooseSvControlRepository.findByDevice(device);
	}

	public void save(DataSet dataSet){
		dataSetRepository.save(dataSet);
	}

	public List<DataItem> findDataItems(){
		List<DataItem> result = (List<DataItem>) dataItemRepository.findAll();
		result.removeIf(dataItem -> dataItem.getFcda().getDevice() == null || dataItem.getFcda().getDevice().isIgnored());
		return result;
	}

	@Transactional
	public void deleteDataItemsByDataSet(List<DataSet> dataSets){
		dataItemRepository.deleteByDataSetIn(dataSets);
	}

	@Transactional
	public void deleteAllDataItems() {
		dataItemRepository.deleteAll();
	}

	public void save(List<DataItem> dataItems){
		dataItemRepository.saveAll(dataItems);
	}

	public void saveAll(List<DataSet> dataSets){
		dataSetRepository.saveAll(dataSets);
	}

	@Transactional
	public void deleteByDevice(Device device){
		dataSetRepository.deleteByDevice(device);
	}

	@Transactional
	public void delete(DataSet dataSet){
		reportControlRepository.deleteByDataSet(dataSet);
		gooseSvControlRepository.deleteByDataSet(dataSet);
		dataSetRepository.delete(dataSet);
	}

	@Transactional
	public void deleteReportControls(List<ReportControl> reportControls){
		reportControlRepository.deleteAll(reportControls);
	}

	public void saveReportControls(List<ReportControl> reportControls){
		reportControlRepository.saveAll(reportControls);
	}

	public List<ReportControl> findReportControl(){
		List<ReportControl> result = (List<ReportControl>) reportControlRepository.findAll();
		result.removeIf(reportControl -> reportControl.getDevice() == null || reportControl.getDevice().isIgnored());
		return result;
	}

	public ReportControl findReportControlByDataSet(DataSet dataSet) {
		return reportControlRepository.findByDataSet(dataSet);
	}

	@Transactional
	public void deleteGooseSvControls(List<GooseSvControl> gooseSvControls) {
		gooseSvControlRepository.deleteAll(gooseSvControls);
	}

	public void saveGooseSvControls(List<GooseSvControl> gooseSvControls) {
		gooseSvControlRepository.saveAll(gooseSvControls);
	}

	public List<GooseSvControl> findGSVControl(){
		List<GooseSvControl> result = (List<GooseSvControl>) gooseSvControlRepository.findAll();
		result.removeIf(gooseSvControl -> gooseSvControl.getDevice() == null || gooseSvControl.getDevice().isIgnored());
		return result;
	}

	public GooseSvControl findGooseSvControlByDataSet(DataSet dataSet) {
		return gooseSvControlRepository.findByDataSet(dataSet);
	}
}
