/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.cumulocity.sdk.client.cep;

import com.cumulocity.rest.representation.CumulocityMediaType;
import com.cumulocity.rest.representation.cep.CepMediaType;
import com.cumulocity.rest.representation.cep.CepModuleCollectionRepresentation;
import com.cumulocity.rest.representation.cep.CepModuleRepresentation;
import com.cumulocity.sdk.client.PagedCollectionResourceImpl;
import com.cumulocity.sdk.client.RestConnector;

public class CepModuleCollectionImpl
        extends PagedCollectionResourceImpl<CepModuleRepresentation, CepModuleCollectionRepresentation, PagedCepModuleCollectionRepresentation>
        implements CepModuleCollection {

    public CepModuleCollectionImpl(RestConnector restConnector, String url, int pageSize) {
        super(restConnector, url, pageSize);
    }

    @Override
    protected CumulocityMediaType getMediaType() {
        return CepMediaType.CEP_MODULE_COLLECTION;
    }

    @Override
    protected Class<CepModuleCollectionRepresentation> getResponseClass() {
        return CepModuleCollectionRepresentation.class;
    }

    @Override
    protected PagedCepModuleCollectionRepresentation wrap(CepModuleCollectionRepresentation collection) {
        return new PagedCepModuleCollectionRepresentation(collection, this);
    }
}
