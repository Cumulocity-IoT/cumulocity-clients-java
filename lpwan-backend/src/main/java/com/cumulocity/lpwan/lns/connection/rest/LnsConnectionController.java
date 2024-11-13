/*
 * Copyright (c) 2024 Cumulocity GmbH, Düsseldorf, Germany and/or its affiliates and/or their licensors. *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Cumulocity GmbH.
 */

package com.cumulocity.lpwan.lns.connection.rest;

import com.cumulocity.lpwan.exception.LpwanServiceException;
import com.cumulocity.lpwan.lns.connection.model.LnsConnection;
import com.cumulocity.lpwan.lns.connection.service.LnsConnectionService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collection;

@RestController
public class LnsConnectionController {

    @Autowired
    LnsConnectionService lnsConnectionService;

    @GetMapping(value = "/lns-connection", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(LnsConnection.PublicView.class)
    public @ResponseBody @NotNull Collection<LnsConnection> get() throws LpwanServiceException {
        return lnsConnectionService.getAll();
    }

    @GetMapping(value = "/lns-connection/{lnsConnectionName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(LnsConnection.PublicView.class)
    public @ResponseBody @NotNull LnsConnection get(@PathVariable @NotBlank String lnsConnectionName) throws LpwanServiceException {
        return lnsConnectionService.getByName(lnsConnectionName);
    }

    @PostMapping(value = "/lns-connection", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @JsonView(LnsConnection.PublicView.class)
    public @ResponseBody @NotNull LnsConnection create(@RequestBody @NotNull LnsConnection lnsConnection) throws LpwanServiceException {
        return lnsConnectionService.create(lnsConnection);
    }

    @PutMapping(value = "/lns-connection/{existingLnsConnectionName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @JsonView(LnsConnection.PublicView.class)
    public @ResponseBody @NotNull LnsConnection update(@PathVariable @NotBlank String existingLnsConnectionName, @RequestBody @NotNull LnsConnection lnsConnectionToUpdate) throws LpwanServiceException {
        return lnsConnectionService.update(existingLnsConnectionName, lnsConnectionToUpdate);
    }

    @DeleteMapping(value = "/lns-connection/{lnsConnectionName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NotNull String lnsConnectionName) throws LpwanServiceException {
        lnsConnectionService.delete(lnsConnectionName);
    }

    @GetMapping(value = "/lns-connection/{lnsConnectionName}/device")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody @NotNull ResponseEntity<Resource> downloadCsvForDeviceMoList(@PathVariable @NotBlank String lnsConnectionName) throws LpwanServiceException {
        InputStreamResource resource = lnsConnectionService.getDeviceManagedObjectsInCsv(lnsConnectionName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=DeviceList.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
