/**

 The ISAconverter, ISAvalidator & BII Management Tool are components of the ISA software suite (http://www.isa-tools.org)

 Exhibit A
 The ISAconverter, ISAvalidator & BII Management Tool are licensed under the Mozilla Public License (MPL) version
 1.1/GPL version 2.0/LGPL version 2.1

 "The contents of this file are subject to the Mozilla Public License
 Version 1.1 (the "License"). You may not use this file except in compliance with the License.
 You may obtain copies of the Licenses at http://www.mozilla.org/MPL/MPL-1.1.html.

 Software distributed under the License is distributed on an "AS IS"
 basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 License for the specific language governing rights and limitations
 under the License.

 The Original Code is the ISAconverter, ISAvalidator & BII Management Tool.

 The Initial Developer of the Original Code is the ISA Team (Eamonn Maguire, eamonnmag@gmail.com;
 Philippe Rocca-Serra, proccaserra@gmail.com; Susanna-Assunta Sansone, sa.sanson@gmail.com;
 http://www.isa-tools.org). All portions of the code written by the ISA Team are Copyright (c)
 2007-2011 ISA Team. All Rights Reserved.

 Contributor(s):
 Rocca-Serra P, Brandizi M, Maguire E, Sklyar N, Taylor C, Begley K, Field D,
 Harris S, Hide W, Hofmann O, Neumann S, Sterk P, Tong W, Sansone SA. ISA software suite:
 supporting standards-compliant experimental annotation and enabling curation at the community level.
 Bioinformatics 2010;26(18):2354-6.

 Alternatively, the contents of this file may be used under the terms of either the GNU General
 Public License Version 2 or later (the "GPL") - http://www.gnu.org/licenses/gpl-2.0.html, or
 the GNU Lesser General Public License Version 2.1 or later (the "LGPL") -
 http://www.gnu.org/licenses/lgpl-2.1.html, in which case the provisions of the GPL
 or the LGPL are applicable instead of those above. If you wish to allow use of your version
 of this file only under the terms of either the GPL or the LGPL, and not to allow others to
 use your version of this file under the terms of the MPL, indicate your decision by deleting
 the provisions above and replace them with the notice and other provisions required by the
 GPL or the LGPL. If you do not delete the provisions above, a recipient may use your version
 of this file under the terms of any one of the MPL, the GPL or the LGPL.

 Sponsors:
 The ISA Team and the ISA software suite have been funded by the EU Carcinogenomics project
 (http://www.carcinogenomics.eu), the UK BBSRC (http://www.bbsrc.ac.uk), the UK NERC-NEBC
 (http://nebc.nerc.ac.uk) and in part by the EU NuGO consortium (http://www.nugo.org/everyone).

 */

package org.isatools.isatab_v1.mapping;

import org.isatools.isatab.mapping.StudyComponentTabMapper;
import org.isatools.isatab_v1.mapping.properties.ProtocolComponentMappingHelper;
import org.isatools.isatab_v1.mapping.properties.ProtocolParameterMappingHelper;
import org.isatools.tablib.exceptions.TabInternalErrorException;
import org.isatools.tablib.exceptions.TabInvalidValueException;
import org.isatools.tablib.mapping.ClassTabMapper;
import org.isatools.tablib.mapping.MappingUtils;
import org.isatools.tablib.mapping.properties.StringPropertyMappingHelper;
import org.isatools.tablib.schema.SectionInstance;
import org.isatools.tablib.utils.BIIObjectStore;
import uk.ac.ebi.bioinvindex.model.Protocol;
import uk.ac.ebi.bioinvindex.model.Study;
import uk.ac.ebi.bioinvindex.model.term.ProtocolType;

import java.util.List;

/**
 * Maps protocols in Investigation file
 * TODO: we need an accession field if we want to recycle protocols.
 * <p/>
 * Dec 2007
 *
 * @author brandizi
 */
public class ProtocolTabMapper extends ClassTabMapper<Protocol> implements StudyComponentTabMapper {
    private MappingUtils mappingUtils;
    private Study mappedStudy;

    @SuppressWarnings("unchecked")
    public ProtocolTabMapper(BIIObjectStore store, SectionInstance sectionInstance) {
        super(store, sectionInstance);


        mappingHelpersConfig.put("Study Protocol Name", new MappingHelperConfig<StringPropertyMappingHelper>(
                StringPropertyMappingHelper.class, new String[][]{{"propertyName", "name"}}
        ));
        mappingHelpersConfig.put("Study Protocol Description", new MappingHelperConfig<StringPropertyMappingHelper>(
                StringPropertyMappingHelper.class, new String[][]{{"propertyName", "description"}}
        ));
        mappingHelpersConfig.put("Study Protocol URI", new MappingHelperConfig<StringPropertyMappingHelper>(
                StringPropertyMappingHelper.class, new String[][]{{"propertyName", "uri"}}
        ));
        mappingHelpersConfig.put("Study Protocol Version", new MappingHelperConfig<StringPropertyMappingHelper>(
                StringPropertyMappingHelper.class, new String[][]{{"propertyName", "version"}}
        ));
        mappingHelpersConfig.put("Study Protocol Parameters Name", new MappingHelperConfig<ProtocolParameterMappingHelper>(
                ProtocolParameterMappingHelper.class, (String[][]) null
        ));
        mappingHelpersConfig.put("Study Protocol Components Name", new MappingHelperConfig<ProtocolComponentMappingHelper>(
                ProtocolComponentMappingHelper.class, (String[][]) null
        ));

        mappingUtils = new MappingUtils(store);
    }


    public Protocol map(int recordIndex) {
        // Basic properties
        Protocol proto = super.map(recordIndex);

        // Type
        //
        ProtocolType type = mappingUtils.createOntologyEntry(
                this.getSectionInstance(), recordIndex,
                "Study Protocol Type Term Accession Number", "Study Protocol Type", "Study Protocol Type Term Source REF",
                ProtocolType.class);
        if (type != null) {
            proto.setType(type);
        }

        // Associate to the study
        //
        if (mappedStudy == null) {
            throw new TabInternalErrorException(
                    "Protocol mapper: No study to attach this protocol to, protocol:" + proto
            );
        }

        // Accession. Although the model package supports protocol reuse, we've been asked to not
        // have this in ISATABs
        proto.setAcc(getStoreKey(proto));

        mappedStudy.addProtocol(proto);


        // Other stuff
        log.trace("New mapped protocol: " + proto);
        return proto;
    }


    public Protocol newMappedObject() {
        return new Protocol(null, null);
    }


    @Override
    public String getStoreKey(Protocol mappedObject) {
        if (mappedObject == null) {
            return null;
        }

        if (mappedStudy == null) {
            throw new TabInternalErrorException("Attempt to map a Protocol without a study associated: " + mappedObject.getName());
        }

        return mappedStudy.getAcc() + "\\" + mappedObject.getName();
    }


    @Override
    public void store(BIIObjectStore store, Protocol proto) {
        if (store.getType(Protocol.class, getStoreKey(proto)) != null) {
            throw new TabInvalidValueException(
                    "Protocol \"" + proto.getName() + "\" defined more than once for the study " + mappedStudy.getAcc());
        }
        super.store(store, proto);
    }


    public void setMappedStudy(Study mappedStudy) {
        this.mappedStudy = mappedStudy;
    }


    @Override
    public List<Integer> getMatchedFieldIndexes() {
        List<Integer> result = super.getMatchedFieldIndexes();
        addMatchedField("Study Protocol Type", result);
        addMatchedField("Study Protocol Type Term Accession Number", result);
        addMatchedField("Study Protocol Type Term Source REF", result);
        return result;
    }


}