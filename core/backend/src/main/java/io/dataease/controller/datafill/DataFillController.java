package io.dataease.controller.datafill;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.dataease.commons.utils.AuthUtils;
import io.dataease.commons.utils.PageUtils;
import io.dataease.commons.utils.Pager;
import io.dataease.controller.ResultHolder;
import io.dataease.controller.request.datafill.*;
import io.dataease.controller.response.datafill.DataFillFormTableDataResponse;
import io.dataease.dto.datafill.DataFillCommitLogDTO;
import io.dataease.dto.datafill.DataFillFormDTO;
import io.dataease.dto.datafill.DataFillTaskDTO;
import io.dataease.dto.datafill.DataFillUserTaskDTO;
import io.dataease.plugins.common.base.domain.DataFillFormWithBLOBs;
import io.dataease.plugins.common.dto.datafill.ExtTableField;
import io.dataease.service.datafill.DataFillDataService;
import io.dataease.service.datafill.DataFillLogService;
import io.dataease.service.datafill.DataFillService;
import io.dataease.service.datafill.DataFillTaskService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@ApiIgnore
@RequestMapping("dataFilling")
@RestController
public class DataFillController {

    @Resource
    private DataFillService dataFillService;
    @Resource
    private DataFillLogService dataFillLogService;
    @Resource
    private DataFillTaskService dataFillTaskService;
    @Resource
    private DataFillDataService dataFillDataService;


    @ApiIgnore
    @PostMapping("/form/save")
    public ResultHolder saveForm(@RequestBody DataFillFormWithBLOBs dataFillForm) throws Exception {
        return dataFillService.saveForm(dataFillForm);
    }

    @ApiIgnore
    @PostMapping("/form/update")
    public ResultHolder updateForm(@RequestBody DataFillFormWithBLOBs dataFillForm) throws Exception {
        return dataFillService.updateForm(dataFillForm);
    }

    @PostMapping("/manage/form/{id}")
    public DataFillFormDTO getWithPrivileges(@PathVariable String id) throws Exception {
        return dataFillService.getWithPrivileges(id);
    }

    @PostMapping("/form/get/{id}")
    public DataFillFormWithBLOBs get(@PathVariable String id) throws Exception {
        return dataFillService.get(id);
    }

    @ApiIgnore
    @PostMapping("/form/delete/{id}")
    public void saveForm(@PathVariable String id) throws Exception {
        dataFillService.deleteForm(id);
    }

    @ApiOperation("查询树")
    @PostMapping("/form/tree")
    public List<DataFillFormDTO> tree(@RequestBody DataFillFormRequest request) {
        return dataFillService.tree(request);
    }

    @ApiIgnore
    @PostMapping("/form/{id}/tableData")
    public DataFillFormTableDataResponse tableData(@PathVariable String id, @RequestBody DataFillFormTableDataRequest request) throws Exception {
        request.setId(id);
        return dataFillDataService.listData(request);
    }

    @ApiIgnore
    @PostMapping("/form/fields/{id}")
    public List<ExtTableField> listFields(@PathVariable String id) throws Exception {
        return dataFillService.listFields(id);
    }

    @ApiIgnore
    @PostMapping("/form/{formId}/delete/{id}")
    public void deleteRowData(@PathVariable String formId, @PathVariable String id) throws Exception {
        dataFillDataService.deleteRowData(formId, id);
    }

    @ApiIgnore
    @PostMapping("/form/{formId}/rowData/save")
    public String newRowData(@PathVariable String formId, @RequestBody Map<String, Object> data) throws Exception {
        return dataFillDataService.updateRowData(formId, null, data, true);
    }

    @ApiIgnore
    @PostMapping("/form/{formId}/rowData/save/{id}")
    public String updateRowData(@PathVariable String formId, @PathVariable String id, @RequestBody Map<String, Object> data) throws Exception {
        return dataFillDataService.updateRowData(formId, id, data, false);
    }


    @ApiIgnore
    @PostMapping("/form/{formId}/commitLog/{goPage}/{pageSize}")
    public Pager<List<DataFillCommitLogDTO>> commitLogs(@PathVariable String formId, @PathVariable int goPage, @PathVariable int pageSize, @RequestBody DataFillCommitLogSearchRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        List<DataFillCommitLogDTO> logs = dataFillLogService.commitLogs(formId, request);

        return PageUtils.setPageInfo(page, logs);
    }

    @ApiIgnore
    @PostMapping("/form/{formId}/task/{goPage}/{pageSize}")
    public Pager<List<DataFillTaskDTO>> tasks(@PathVariable String formId, @PathVariable int goPage, @PathVariable int pageSize, @RequestBody DataFillTaskSearchRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        List<DataFillTaskDTO> tasks = dataFillTaskService.tasks(formId, request);

        return PageUtils.setPageInfo(page, tasks);
    }

    @ApiIgnore
    @PostMapping("/form/{formId}/task/save")
    public void saveTask(@PathVariable String formId, @RequestBody DataFillTaskSearchRequest request) throws Exception {

        dataFillTaskService.saveTask(formId, request);

    }

    @ApiIgnore
    @PostMapping("/form/task/{taskId}/delete")
    public void deleteTask(@PathVariable Long taskId) {

        dataFillTaskService.deleteTask(taskId);

    }

    @ApiIgnore
    @PostMapping("/form/task/{taskId}/enable")
    public void enableTask(@PathVariable Long taskId) throws Exception {

        dataFillTaskService.enableTask(taskId);

    }

    @ApiIgnore
    @PostMapping("/form/task/{taskId}/disable")
    public void disableTask(@PathVariable Long taskId) throws Exception {

        dataFillTaskService.disableTask(taskId);

    }

    @ApiIgnore
    @PostMapping("/myTask/{type}/{goPage}/{pageSize}")
    public Pager<List<DataFillUserTaskDTO>> userTasks(@PathVariable String type, @PathVariable int goPage, @PathVariable int pageSize, @RequestBody DataFillUserTaskSearchRequest request) {
        Long userId = AuthUtils.getUser().getUserId();
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        List<DataFillUserTaskDTO> tasks = dataFillTaskService.userTasks(userId, type, request);

        return PageUtils.setPageInfo(page, tasks);
    }


    @ApiIgnore
    @PostMapping("/myTask/fill/{taskId}")
    public void userFillData(@PathVariable String taskId, @RequestBody Map<String, Object> data) throws Exception {
        dataFillService.fillFormData(taskId, data);
    }

}
