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

package org.isatools.isatab.isaconfigurator.validators;

import org.apache.commons.lang.StringUtils;
import org.isatools.isatab.configurator.schema.FieldType;
import org.isatools.isatab.configurator.schema.IsaTabConfigurationType;
import org.isatools.isatab.configurator.schema.UnitFieldType;
import org.isatools.isatab.gui_invokers.GUIInvokerResult;
import org.isatools.isatab.isaconfigurator.ISAConfigurationSet;
import org.isatools.tablib.schema.Field;
import org.isatools.tablib.schema.Record;
import org.isatools.tablib.schema.SectionInstance;
import org.isatools.tablib.utils.BIIObjectStore;

import java.util.List;
import java.util.Set;

/**
 * Validates the unit used for a certain field and its values.
 *
 * @author brandizi
 *         <b>date</b>: Nov 5, 2009
 */
@SuppressWarnings("static-access")
public class UnitFieldsValidator extends AbstractValidatorComponent {
    public UnitFieldsValidator(BIIObjectStore store, ISAConfigurationSet isaConfigSet, Set<String> messages) {
        super(store, isaConfigSet, messages);
    }

    @Override
    public GUIInvokerResult validate(SectionInstance table, IsaTabConfigurationType cfg) {
        boolean result = true;

        List<Field> fields = table.getFields();
        int nfields = fields.size();

        for (Field field : fields) {
            String header = field.getAttr("header");
            FieldType cfield = isaConfigSet.getConfigurationField(cfg, header);
            if (cfield == null) {
                continue;
            }

            UnitFieldType ucfield = isaConfigSet.getUnitField(cfield);
            if (ucfield == null) {
                continue;
            }

            int fcol = field.getIndex();
            Field rfield = fcol == nfields ? null : fields.get(field.getIndex() + 1);
            if (ucfield.getIsRequired()) {
                if (rfield == null || !"Unit".equalsIgnoreCase(rfield.getAttr("header"))) {
                    log.warn("The field '" + header + "' in the file '" + table.getFileId() + "' misses a required 'Unit' column");
                    result = false;
                } else {
                    // check the values too
                    for (Record record : table.getRecords()) {
                        result &= validateUnitValue(record, fcol, cfg, cfield);
                    }
                }
            }
            // TODO: ontology checking
        }
        return result ? GUIInvokerResult.SUCCESS : GUIInvokerResult.WARNING;
    }


    private boolean validateUnitValue(Record record, int icol, IsaTabConfigurationType cfg, FieldType cfgField) {
        boolean result = true;
        String file = record.getParent().getFileId();

        if (icol == record.size()) {
            return true;
        }
        Field field = record.getParent().getField(icol);
        String val = StringUtils.trimToNull(record.getString(icol));

        String header = field.getAttr("header");
        FieldType cfield = isaConfigSet.getConfigurationField(cfg, header);
        if (cfield == null) {
            return true;
        }

        UnitFieldType ucfield = isaConfigSet.getUnitField(cfield);
        if (ucfield == null) {
            return true;
        }

        int icol1 = icol + 1;
        Field rfield = record.getParent().getField(icol + 1);

        // The need for the unit is checked elsewhere
        if (!"Unit".equalsIgnoreCase(rfield.getId())) {
            return true;
        }

        String uval = StringUtils.trimToNull(record.getString(icol1));

        if (val == null) {
            if (uval != null) {
                log.warn("Field '" + header + "' has a unit but not a value in the file '" + file + "'");
                return true;
            }
            // If both the value and the unit are OK, go ahead, we check only unit here
            return false;
        }


        if (ucfield.getIsRequired() && uval == null) {
            log.debug(
                    "The field '" + header + "' misses the required unit for the value '" + val + "' in the file '" + file + "'"
            );
            messages.add("Missing Unit Values for the field '" + header + "' in the file '" + file + "'");
            result = false;
        }

        return result;
    }

}
