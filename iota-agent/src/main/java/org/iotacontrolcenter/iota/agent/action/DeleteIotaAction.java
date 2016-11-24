package org.iotacontrolcenter.iota.agent.action;

import org.iotacontrolcenter.dto.ActionResponse;
import org.iotacontrolcenter.properties.source.PropertySource;

public class DeleteIotaAction extends AbstractAction implements IotaAction  {

    public DeleteIotaAction() {
        super(new String[] { PropertySource.IOTA_APP_DIR_PROP });
    }

    @Override
    public ActionResponse execute() {
        preExecute();
        ActionResponse resp = new ActionResponse(true, "happy");
        return resp;
    }

}
