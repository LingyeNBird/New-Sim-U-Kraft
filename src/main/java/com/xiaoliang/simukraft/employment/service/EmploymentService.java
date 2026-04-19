package com.xiaoliang.simukraft.employment.service;

public interface EmploymentService {
    EmploymentResult hire(EmploymentCommands.HireCommand command);

    EmploymentResult fireByNpc(EmploymentCommands.FireByNpcCommand command);

    EmploymentResult fireByWorkplace(EmploymentCommands.FireByWorkplaceCommand command);

    EmploymentResult onWorkBlockRemoved(EmploymentCommands.WorkBlockRemovedCommand command);
}

