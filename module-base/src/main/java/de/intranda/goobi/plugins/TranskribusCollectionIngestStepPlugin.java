package de.intranda.goobi.plugins;

import java.io.IOException;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

@PluginImplementation
@Log4j2
public class TranskribusCollectionIngestStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_transkribus_collection_ingest";
    @Getter
    private Step step;
    private String transkribusLogin;
    private String transkribusPassword;
    private String transkribusApiUrl;
    private String transkribusCollection;
    private String metsUrl;
    private String returnPath;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig("intranda_step_transkribus_collection", step);
        transkribusLogin = myconfig.getString("transkribusLogin");
        transkribusPassword = myconfig.getString("transkribusPassword");
        transkribusApiUrl = myconfig.getString("transkribusApiUrl");
        transkribusCollection = myconfig.getString("transkribusCollection");
        metsUrl = myconfig.getString("metsUrl");
        log.info("TranskribusCollection step plugin initialized");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_transkribus_collection.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {

        System.out.println("login: " + transkribusLogin);
        System.out.println("transkribusPassword: " + transkribusPassword);
        System.out.println("transkribusApiUrl: " + transkribusApiUrl);
        System.out.println("transkribusCollection: " + transkribusCollection);
        System.out.println("metsUrl: " + metsUrl);

        try {

            // construct the viewer URL for the METS file and make sure it is reachable
            Prefs prefs = step.getProzess().getRegelsatz().getPreferences();
            Fileformat ff = step.getProzess().readMetadataFile();
            DigitalDocument dd = ff.getDigitalDocument();
            VariableReplacer vr = new VariableReplacer(dd, prefs, step.getProzess(), null);
            String url = vr.replace(metsUrl);
            boolean urlStatusOk = TranskribusHelper.checkUrl(url);
            if (!urlStatusOk) {
                log.error("TranskribusCollection - METS URL not accessible: " + url);
                Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR,
                        "Ingest into Transkribus collection canceled because the METS URL is not accessible: " + url);
                return PluginReturnValue.ERROR;
            }

            // Login into Transkribus and get a session ID

            // Ingest METS file to Trandskribus and retrieve a Document ID back

            // Store Document ID as Process Property

            // Write Success message with Document ID into the Journal

            log.info("TranskribusCollection step plugin executed");
            return PluginReturnValue.FINISH;
        } catch (ReadException | IOException | SwapException | PreferencesException e) {
            log.error("TranskribusCollection - Error while creeatin the ingest", e);
            Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR,
                    "Ingest into Transkribus collection canceled because of an unexpected exception: " + e.getMessage());
            return PluginReturnValue.ERROR;
        }
    }
}
