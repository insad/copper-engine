package de.scoopgmbh.copper.monitoring.client.ui.workflowinstance.result;

import javafx.scene.layout.BorderPane;
import de.scoopgmbh.copper.monitoring.client.context.FormContext;
import de.scoopgmbh.copper.monitoring.client.form.FxmlForm;
import de.scoopgmbh.copper.monitoring.client.form.filter.FilterAbleForm;
import de.scoopgmbh.copper.monitoring.client.form.filter.FilterResultController;
import de.scoopgmbh.copper.monitoring.client.ui.audittrail.filter.AuditTrailFilterModel;
import de.scoopgmbh.copper.monitoring.client.ui.audittrail.result.AuditTrailResultModel;
import de.scoopgmbh.copper.monitoring.client.ui.worklowinstancedetail.filter.WorkflowInstanceDetailFilterModel;
import de.scoopgmbh.copper.monitoring.client.ui.worklowinstancedetail.result.WorkflowInstanceDetailResultModel;
import de.scoopgmbh.copper.monitoring.core.model.ProcessingEngineInfo;

public class BaseWorkflowInstanceListNavigation implements WorkflowInstanceListNavigation {

	FormContext formContext;
	public BaseWorkflowInstanceListNavigation(FormContext formContext) {
		super();
		this.formContext = formContext;
	}

	@Override
	public void navigateToAudittrail(String workflowInstanceId) {
		FilterAbleForm<AuditTrailFilterModel,AuditTrailResultModel> audittrailForm = formContext.createAudittrailForm();
		audittrailForm.getFilter().workflowInstanceId.set(workflowInstanceId);
		audittrailForm.show();
	}

	@Override
	public void navigateToIntsanceDetail(String workflowInstanceId, ProcessingEngineInfo engine) {
		formContext.createWorkflowInstanceDetailForm(workflowInstanceId,engine).show();
	}

	@Override
	public FxmlForm<FilterResultController<WorkflowInstanceDetailFilterModel, WorkflowInstanceDetailResultModel>> createWorkflowinstanceDetailResultForm(
			BorderPane detailPane) {
		return formContext.createWorkflowinstanceDetailResultForm(detailPane);
	}
	
}