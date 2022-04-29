package com.sabre.devops.lodging

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.soap.Node
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

class Utils {
			
    Script script;
	
	public String getHTMLRows(def dependencyConflictsSummary, def duplicateClassesSummary){
		String tableRows = ""
		for(String key : dependencyConflictsSummary.keySet()){
			for(def compConflict : dependencyConflictsSummary.get(key)){
				def classConflict = duplicateClassesSummary.get(key)
				String newLineSeparateDepConflicts = ""
				String newLineSeparatedClassConflicts = ""
				for(def conflict : compConflict.conflicts){
					newLineSeparateDepConflicts = newLineSeparateDepConflicts + conflict + "</br></br>"
				}
				for(def conflict : classConflict.conflicts){
					newLineSeparatedClassConflicts = newLineSeparatedClassConflicts + conflict + "</br></br>"
				}
				tableRows = tableRows + "<tr><td align=\"center\" valign=\"center\">" + key + "</td><td align=\"center\" valign=\"center\">" + compConflict.count + "</td><td align=\"left\" valign=\"top\">" + newLineSeparateDepConflicts + "</td><td align=\"center\" valign=\"center\">" + classConflict.count + "</td><td align=\"left\" valign=\"top\">" + newLineSeparatedClassConflicts + "</td></tr>"
			}
		}
		return tableRows
	}

	public def processDuplicateClasses(String duplicateClasses ) {
		String[] lines = duplicateClasses.split("\\r?\\n");
		Map<String, Conflicts> duplicateClassesSummary = new HashMap<String, Conflicts>();
		String componentName = ""
		def conflicts =  new Conflicts();
		def booleanFoundIn = false
		def conflict = ""
		for(String line : lines){
			if(line.contains("[INFO] Building ")){
				if(componentName != ""){
					duplicateClassesSummary.put(componentName,conflicts)
					conflicts =  new Conflicts();
				}
				componentName = line.replace("[INFO] Building ", "")
			}
			if(line.startsWith("  Found in:")){
				conflicts.count++
				booleanFoundIn = true
			} else if(line.startsWith("  Duplicate classes:")){
				conflicts.addToConflicts(conflict.substring(0,conflict.length() - 5))
				conflict = ""
				booleanFoundIn = false
			} else if(booleanFoundIn){
				conflict = conflict + line.trim() + " --> "
			}
		}
		return duplicateClassesSummary
	}

    public int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }


	public def processDependencyConflicts(String dependencyConflicts){
		String[] lines = dependencyConflicts.split("\\r?\\n");
		Map<String, Conflicts> dependencyConflictsSummary = new HashMap<String, Conflicts>();
		String componentName = ""
		def conflicts =  new Conflicts();
		for(String line : lines){
			//script.echo(line)
			if(line.contains("[INFO] Building ")){
				if(componentName != ""){
					//script.echo("inside component name if of processDependencyConflicts")
					dependencyConflictsSummary.put(componentName,conflicts)
					conflicts = new Conflicts();
				}
				//componentName = line.split("-< ")[1].split(" >-")[0]
				componentName = line.replace("[INFO] Building ", "")
			}
			if(line.startsWith("Dependency convergence error for ")){
				conflicts.count++
				line = line.replaceAll("Dependency convergence error for ", "").split(" paths")[0]
				conflicts.addToConflicts(line.substring(0, line.lastIndexOf(":")))
			}
		}
		return dependencyConflictsSummary
	}
    
    def getAllActiveSamplersFromAJmxFile(def path){
        def activeSamplerTypes = "JSR223Sampler,HTTPSamplerProxy"
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder()
        Document doc = docBuilder.parse(path)
        Map<String,String> map = new HashMap<String,String>()
        ArrayList<String> namesOfSamplersThatAreActive = new ArrayList<String>()
        ArrayList<String> activeThroughputControllers = new ArrayList<String>()
        NodeList threadGroupNodeList = doc.getElementsByTagName("ThreadGroup");
        for (int i=0; i<threadGroupNodeList.getLength(); i++) {
            Element threadGroupNode = (Element) threadGroupNodeList.item(i)
            if(evalauteThreadGroupForFurtherProcessing(threadGroupNode)) {
                String threadGroupName=threadGroupNode.getAttribute("testname")
                map.put("threadGroupName", threadGroupName)
                getActiveNodesUnderAParentNode(doc,"ThreadGroup",threadGroupName,activeSamplerTypes,namesOfSamplersThatAreActive)
                getActiveNodesUnderAParentNode(doc,"ThreadGroup",threadGroupName,"ThroughputController",activeThroughputControllers)
                for(String activeThroughputController : activeThroughputControllers) {
                    getActiveNodesUnderAParentNode(doc,"ThroughputController",activeThroughputController,activeSamplerTypes,namesOfSamplersThatAreActive)
                    ArrayList<String> activeParallelSamplers = new ArrayList<String>()
                    getActiveNodesUnderAParentNode(doc,"ThroughputController",activeThroughputController,"com.blazemeter.jmeter.controller.ParallelSampler",activeParallelSamplers)
                    for(String activeParallelSampler : activeParallelSamplers) {
                        getActiveNodesUnderAParentNode(doc,"com.blazemeter.jmeter.controller.ParallelSampler",activeParallelSampler,activeSamplerTypes,namesOfSamplersThatAreActive)
                    }
                    ArrayList<String> activeNestedThroughputControllers = new ArrayList<String>()
                    getActiveNodesUnderAParentNode(doc,"ThroughputController",activeThroughputController,"ThroughputController",activeNestedThroughputControllers)
                    for(String activeNestedThroughputController : activeNestedThroughputControllers) {
                        getActiveNodesUnderAParentNode(doc,"ThroughputController",activeNestedThroughputController,activeSamplerTypes,namesOfSamplersThatAreActive)
                    }
                }
            }
        }
        map.put("samplerName",namesOfSamplersThatAreActive.join(","))
        return map;
    }
    
    def evalauteThreadGroupForFurtherProcessing(Element threadGroupNode) {
        def returnValue = false
        if (threadGroupNode.getAttribute("testname") != "ReadRequest" && threadGroupNode.getAttribute("enabled") == "true") {
           returnValue = true
        }
        return returnValue
    }
    
    def getNextSibling(Document doc, def tagName, def testName) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String xpathQuery= "//" + tagName + "[@testname=\"" + testName + "\"]/following-sibling::*"
        XPathExpression expr = xpath.compile(xpathQuery);
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result
        return nodes.item(0)
    }
    
    def getActiveNodesUnderAParentNode(Document doc, def parentNodeName, def testName, String nodeName, ArrayList<String> testNamesOfTagsThatAreActive) {
        //def testNamesOfTagsThatAreActive = new ArrayList()  
        def hashNode = getNextSibling(doc, parentNodeName, testName)
        try {
            NodeList hashNodeChildren=hashNode.getChildNodes();
            for (int i=0; i<hashNodeChildren.getLength(); i++) {
                def hashNodeChild = hashNodeChildren.item(i)
                if(hashNodeChild.getNodeType()==Node.ELEMENT_NODE){
                    Element childElement=(Element) hashNodeChild
                    if(childElement.getAttribute("enabled") == "true" && nodeName.contains(childElement.getTagName())) {
                        testNamesOfTagsThatAreActive.add(childElement.getAttribute("testname"))
                        //break
                    }
                }
            }  
        } catch(java.lang.NullPointerException e) {
            println("Nullpointer Exception Occured while processing " + nodeName + " under " + parentNodeName)
        }      
    }
    
    def getVersions(def component) {
        String versions = getVersionsFromSnapshots(component)
        versions += "," + getVersionsFromReleases(component)
        return versions
    }
    
    def getVersionsFromSnapshots(def component) {
        return getVersions(component, "maven-snapshots")
    }
    
    def getVersionsFromReleases(def component) {
        return getVersions(component, "maven-staging")
    }
    
    def executionNeeded(def branchName,def scmRevision, def previouslyExecutedRevision) {
        return (getExecutionNeededBasedOnSCMRevisions(scmRevision,previouslyExecutedRevision) && executionNeededBasedOnBranchName(branchName))
    }
    
    
    def getExecutionNeededBasedOnSCMRevisions(def scmRevision, def previouslyExecutedRevision) {
        return (previouslyExecutedRevision.contains(scmRevision)) ? false : true
    }
    
    def executionNeededBasedOnBranchName(def branchName) {
        if(branchName.toString().startsWith("dev_") || branchName.toString().startsWith("development_") || branchName.toString().startsWith("rel_20") || branchName.toString().startsWith("release_") || branchName.toString() == "develop" || branchName.toString() == "trunk") {
            return true
        }
        return false
    }
    
    def getVersions(def component, def location) {
        try {
            List<String> versionsList = new ArrayList<String>()
			//"https://repository.sabre.com/repository/maven-snapshots/com/sabre/hotels/nghp/normalization/nghp-product-normalization/maven-metadata.xml" --user", "user:password"
			def artifactsUrl
			def artifactsObjectRaw
	        //artifactsUrl = "http://maven.sabre.com/content/repositories/" + location + "/com/sabre/hotels/nghp/bundles/" + component + "/maven-metadata.xml"
            if(component.startsWith("ngcp-") || component.startsWith("cars-")) {
                artifactsUrl = "https://repository.sabre.com/repository/" + location + "/com/sabre/cars/ngcp/bundles/" + component.replace("ngcp-","") + "/maven-metadata.xml"
            } else if(component.equals("simulator")){
                artifactsUrl = "https://repository.sabre.com/repository/" + location + "/com/sabre/lgs/bundles/" + component + "/maven-metadata.xml"
            }
            else{
                artifactsUrl = "https://repository.sabre.com/repository/" + location + "/com/sabre/hotels/nghp/bundles/" + component + "/maven-metadata.xml"
            }
            return getVersionsList(artifactsUrl)
        }catch(Exception e) {
            return ""
        }
    }

    def getNewVeraCodeAppName(def oldAppName){
        String newAppName = oldAppName;
        switch(oldAppName.toString()) {
            case "Hotel-Manager":
                newAppName = "[EHOTELS:HOTEL_MANAGER_GUI] Hotel Manager";
                break;
            case "Lodging UI":
                newAppName = "[EHOTELS:CONTENT_SERVICES_LODGING_UI] Lodging UI";
                break;
            case "NGHP - Booking":
                newAppName = "[EHOTELS:BOOK_PROC] NGHP-Booking"
                break;
            case "NGHP Aggregator":
                newAppName = "[EHOTELS:NGHP-AGG-GCP] NGHP Aggregator GCP"
                break;
            case "NGHP Connect":
                newAppName = "[EHOTELS:AGG_CONNECT] NGHP Connect"
                break;
            case "Nghp-Content-Indexer":
                newAppName = "[EHOTELS] Nghp-Content-Indexer"
                break;
            case "NGHP Content":
                newAppName = "[EHOTELS] NGHP Content"
                break;
            case "NGHP Content Acquisition":
                newAppName = "[EHOTELS:AGG_CONTENT_ACQ] NGHP Content Acquisition"
                break;
            case "NGHP Distribution":
                newAppName = "[EHOTELS:NGHPDIST] NGHP Distribution"
                break;
            case "NGHP Shopping":
                newAppName = "[EHOTELS:NGHPSHOP] NGHP Shopping"
                break;
            case "NGHP-Connect-GDS":
                newAppName = "[EHOTELS:NGHP-CONNECT-GDS] NGHP-Connect-GDS"
                break;
            case "NGHP-GDS":
                newAppName = "[EHOTELS:NGHP-GDS] NGHP-GDS"
                break;
            case "NGHP-Jobs":
                newAppName = "[EHOTELS:NGHP_JOBS] NGHP Jobs"
                break;
            case "NGHP-Manager":
                newAppName = "[EHOTELS:NGHP_MANAGER_APIS] NGHP-Manager"
                break;
            case "VCMP":
                newAppName = "[EHOTELS] VCMP"
                break;
            case "LGS Simulator":
                newAppName = "[CARSOS:SIM] LGS Simulator"
                break;
            case "nggp-services":
                newAppName = "[GEO:GEO_SVC] nggp-services"
                break;
            case "nggp-job-management":
                newAppName = "[GEO:GEO_PLATFORM_JOBS] NGGP-job-management"
                break;
            case "NGHP-ARI-Acquisition" :
                newAppName = "[EHOTELS:RATES_ACQ] NGHP-ARI-Acquisition"
                break;
            case "nghp-core" :
                newAppName = "[EHOTELS] nghp-core"
                break;
            case "nghp-shopping-indexer" :
                newAppName = "[EHOTELS:SHOP_INDX] nghp-shopping-indexer"
                break;
        }
        return newAppName;
    }

    def getNextReleaseNumber(String componentName, String keyword){
        int i =1;
        Date date = new Date()
        String releaseInitialPart =  date.format("yy.MM")
        String releaseNumber = releaseInitialPart + keyword + i
        script.echo("releaseNumber # " + releaseNumber)
        String takenVersions = getTakenVersions(componentName)
        script.echo("takenVersions # " + takenVersions)
        while(takenVersions.split(",").contains(releaseNumber)){
            releaseNumber = releaseInitialPart + keyword + ++i
        }
        script.echo("releaseNumber # " + releaseNumber)
        return releaseNumber
    }

    def getTakenVersions(def componentName){
        String nexusPath = "";
        switch(componentName.toString().toLowerCase()) {
            case "nghp-commons":
                nexusPath = "com/sabre/hotels/nghp/commons/nghp-commons/"
                break
            case "nghp-currency-conversion":
                nexusPath = "com/sabre/hotels/nghp/nghp-currency-conversion/"
                break
            case "nghp-preferencing-rules":
                nexusPath = "com/sabre/hotels/nghp/preferencing/nghp-preferencing-rules/"
                break;
            case "nghp-product-classifier":
                nexusPath = "com/sabre/hotels/nghp/nlp/nghp-product-classifier/"
                break;
            case "nghp-product-normalization":
                nexusPath = "com/sabre/hotels/nghp/normalization/nghp-product-normalization/"
                break;
            case "nghp-rules":
                nexusPath = "com/sabre/hotels/nghp/rules/nghp-rules/"
                break;
            case "nghp-viewership-rules":
                nexusPath = "com/sabre/hotels/nghp/viewership/nghp-viewership-rules/"
                break;
            case "ehotels-preferencing":
                nexusPath = "com/sabre/ehotels/preferencing/"
                break;
            case "ngcp-commons":
                nexusPath = "com/sabre/cars/ngcp/commons/ngcp-commons/"
                break;
            case "lgs-monitoring":
                nexusPath = "com/sabre/lgs/monitoring/lgs-monitoring/"
                break;
        }
        return getVersionsList("https://repository.sabre.com/repository/maven-staging/" + nexusPath + "maven-metadata.xml")
    }

    def getVersionsList(String artifactsUrl){
        try {
            def artifactsObjectRaw = ["curl", "-s", "-H", "accept: application/xml", "-k", "--url", "${artifactsUrl}", "--user", "pTGqqc0t:RZSMi3PrR5crIKFJoLA9V63uevi0hV8ZGsaLNdjuHTtO"].execute().text
            def xmlSlurper = new XmlSlurper()
            def artifactsXMLObject = xmlSlurper.parseText(artifactsObjectRaw)
            def versionsList = artifactsXMLObject.depthFirst().findAll { it.name() == 'version' }
            return versionsList.sort().join(',').split(",").sort().join(",")
        } catch(Exception e){
            return ""
        }
    }

    def getGitProjectNameForIQScan(String componentName){
        String projectName = "";
        if(componentName.toLowerCase().startsWith("nghp") || componentName.toLowerCase().startsWith("ehotels") || componentName.toLowerCase().startsWith("matip") || componentName.toLowerCase().startsWith("hcp") || componentName.toLowerCase().startsWith("updateserverweb")){
            projectName = "tntlgslg";
        } else if(componentName.toLowerCase().startsWith("ngcp") || componentName.toLowerCase().startsWith("cars") || componentName.toLowerCase().startsWith("car-rate-view")){
            projectName = "tntlgscr";
        } else if(componentName.toLowerCase().startsWith("cruise") || componentName.toLowerCase().startsWith("ngsp") || componentName.toLowerCase().startsWith("ngcrp") || componentName.toLowerCase().startsWith("sailingacquisition") || componentName.toLowerCase().startsWith("itinacquisition")){
            projectName = "tntlgscs";
        }  else if(componentName.toLowerCase().startsWith("lgs")){
            projectName = "tntlgs";
        } else if(componentName.toLowerCase().startsWith("hotels-widget")){
            projectName = "TGHOT";
        }
        return projectName
    }

    def getIQAppNameForIQScan(String componentName){
        String appName = "";
        if(componentName.toLowerCase().equals("cars-widget") ){
            appName = "com.sabre.ngcp-cars";
        } else if(componentName.toLowerCase().startsWith("hotels-widget")){
            appName = "com.sabre.htw-htw";
        } else if(componentName.toLowerCase().startsWith("updateserverweb")){
            appName = "com.sabre.ehotels-UpdateServerWeb";
        } else if(componentName.toLowerCase().startsWith("nghp-commons")){
            appName = "com.sabre.hotels.nghp.commons-nghp-commons";
        } else if(componentName.toLowerCase().startsWith("nghp-currency-conversion")){
            appName = "com.sabre.hotels.nghp-nghp-currency-conversion";
        }  else if(componentName.toLowerCase().startsWith("nghp-preferencing-rules")){
            appName = "com.sabre.hotels.nghp.preferencing-nghp-preferencing-rules";
        }  else if(componentName.toLowerCase().startsWith("nghp-product-classifier")){
            appName = "com.sabre.hotels.nghp.nlp-nghp-product-classifier";
        }  else if(componentName.toLowerCase().startsWith("nghp-product-normalization")){
            appName = "com.sabre.hotels.nghp.normalization-nghp-product-normalization";
        }  else if(componentName.toLowerCase().startsWith("nghp-rules")){
            appName = "com.sabre.hotels.nghp.rules-nghp-rules";
        }  else if(componentName.toLowerCase().startsWith("nghp-viewership-rules")){
            appName = "com.sabre.hotels.nghp.viewership-nghp-viewership-rules";
        }  else if(componentName.toLowerCase().startsWith("ehotels-preferencing")){
            appName = "com.sabre.ehotels-preferencing";
        } else if(componentName.toLowerCase().startsWith("ehotels-logging")){
            appName = "com.sabre.ehotels-logging";
        } else if(componentName.toLowerCase().startsWith("lgs-monitoring")){
            appName = "com.sabre.lgs.monitoring-lgs-monitoring";
        } else if(componentName.toLowerCase().startsWith("ngcp-commons")){
            appName = "com.sabre.cars.ngcp.commons-ngcp-commons";
        } else if(componentName.toLowerCase().startsWith("ehotels-hotel-manager")){
            appName = "com.sabre.lgs.deployment.bundles.hotels-hotel-manager";
        }else if(componentName.toLowerCase().startsWith("nghp") || componentName.toLowerCase().startsWith("ehotels") || componentName.toLowerCase().startsWith("matip") || componentName.toLowerCase().startsWith("hcp")){
            appName = "com.sabre.lgs.deployment.bundles.hotels-" + componentName;
        } else if(componentName.toLowerCase().startsWith("ngcp") || componentName.toLowerCase().startsWith("cars") || componentName.toLowerCase().startsWith("car-rate-view")){
            appName = "com.sabre.lgs.deployment.bundles.cars-" + componentName;
        } else if(componentName.toLowerCase().startsWith("ngsp") || componentName.toLowerCase().startsWith("ngcrp") || componentName.toLowerCase().startsWith("cruise")) {
            appName = "com.sabre.lgs.deployment.bundles.cruise-" + componentName;
        }  else if(componentName.toLowerCase().startsWith("sailingacquisition")) {
            appName = "com.sabre.lgs.deployment.bundles.cruise-ngsp-sailingacquisition";
        }  else if(componentName.toLowerCase().startsWith("itinacquisition")) {
            appName = "com.sabre.lgs.deployment.bundles.cruise-ngsp-itinacquisition";
        } else if(componentName.toLowerCase().startsWith("lgs")){
            appName = "com.sabre.lgs.deployment.bundles-" + componentName;
        }
        return appName
    }

    def getGitRepoNameForIQScan(String componentName){
        String repoName = componentName;
        switch(componentName.toString().toLowerCase()) {
            case "matip":
            case "hcp":
                repoName = "ehotels-xmlsb";
                break;
            case "ehotelsgui":
                repoName = "ehotels-gui-tomcat";
                break;
            case "nghp-core":
                repoName = "ehotels-nghp";
                break;
            case "ngcp-content":
                repoName = "ngcp-content-services"
                break;
            case "ngcp-content-acquisition":
                repoName = "ngcp-acq-services"
                break;
            case "ngcp-connect":
                repoName = "ngcp-connect-services"
                break;
            case "ngcp-booking":
                repoName = "ngcp-booking-services"
                break;
            case "ngcp-distribution":
                repoName = "ngcp-dist-services"
                break;
            case "cars-widget":
                repoName = "ngcp-cars-tg"
                break;
            case "hotels-widget":
                repoName = "htw"
                break;
            case "car-rate-view":
                repoName = "lgs-car-rate-view"
                break;
            case "cruise-misc":
            case "cruise-scheduler":
            case "cruise-services":
                repoName = "cruise-misc"
                break;
            case "updateserverweb":
                repoName = "ehotels_ehtlwebsvc"
                break;
            default:
                repoName = componentName;
                break;
        }
        return repoName
    }

    def getGitProjectName(def componentName){
        String projectName = "tntlgsls";
        switch(componentName.toString().toLowerCase()) {
            case "ngcp-commons":
                projectName = "tntlgscr";
                break;
            case "lgs-monitoring":
                projectName = "tntlgs";
                break;
            default:
                projectName = "tntlgslg";
                break;
        }
        return projectName
    }

    def getRepoURL(def componentName){
        return "https://SG0959205@git.sabre.com/scm/tntlgslg/" + componentName + ".git"
    }
    
    def isReleaseBranch(def branchName, def scm) {
        def isReleaeBranch = "no"
        if(scm.class.toString().contains("SubversionSCM")){
            def svnURL = scm.locations.toString()
            if(svnURL.contains("/releases/")) {
                isReleaeBranch = "yes"
            } 
        } else {
            if(scm.branches[0].name.toString().contains("release/")) {
                isReleaeBranch = "yes"
            }
        }
        if(isReleaeBranch == "no" && branchName.startsWith("rel_")) {
            isReleaeBranch = "yes"
        }  
        return isReleaeBranch      
    }
    
    def getBranchName(def scm) {
        def branchName = ""
        if(scm.class.toString().contains("SubversionSCM")){
            def svnURL = scm.locations.toString()
            if(svnURL.contains("/trunk")){
                branchName = "trunk"
            } else if(svnURL.contains("/branches")) {
                branchName =  svnURL.split("branches/")[1].split("/")[0].split("@")[0]
            } else if(svnURL.contains("/releases")) {
                branchName =  svnURL.split("releases/")[1].split("/")[0].split("@")[0]
            } else if(svnURL.contains("/sandbox")) {
                branchName =  svnURL.split("sandbox/")[1].split("/")[0].split("@")[0]
            } else if(svnURL.contains("/tags")) {
                branchName =  svnURL.split("tags/")[1].split("/")[0].split("@")[0]
            } else {
                branchName = "trunk"
            }
        } else {
            branchName = scm.branches[0].name
            if(branchName.contains("/")) {
                branchName = branchName.substring(branchName.lastIndexOf('/') + 1);
            }
        }
        return branchName
    }
    
    def getJenkinsFolderPath(def scm, def criteria, def workSpace) {
        def path = ""
        if(scm.class.toString().contains("SubversionSCM")){
            path = workSpace
        } else {
			path = getPOMFolderPath(scm,workSpace)
            /*switch(criteria.toString()) {
                case ["csl-ui", "com.sabre.hotels.nghp.search:nghp-hotels-gui", "Lodging UI".toUpperCase()]:
                    path = workSpace +"/lodging-ui/src/logding-ui"
                    break
                default:
                    path = getPOMFolderPath(scm,workSpace) //workSpace +"/src/vcmp-services" //
                    break
            }*/
        }
        return path
    }
    
    def getPOMFolderPath(def scm, def workspace) {
        def path = ""
        if(scm.class.toString().contains("SubversionSCM")){
            path = workspace
        } else {
            path = getPOMFolderPath(workspace)
        }
        return path
    }

    def getPOMFolderPath(def workspace){
        def path = ""
        def pomPath = getSubDiredtoryContainingGivenFile(workspace,"pom.xml")
        if(pomPath == "" || pomPath == null) {
            path = workspace
        } else {
            path = pomPath
        }
        return path
    }
    
    def checkIfRequiredFileIsPresentInCurrentDir(def currentDir, def fileName) {
        def isPresent = false
		script.echo("currentDir # " + currentDir)
        //println "currentDir # $currentDir"
        File[] children = currentDir.listFiles();
        for(def i=0;i < children.size(); i++) {
            //script.echo("FileName #" + children[i].name)
            if(children[i].name == fileName) {
                isPresent = true
                break
            }
        }
        return isPresent
    }

    def deleteGivenFolder(def folderPath){
        def folder = new File(folderPath)
        folder.deleteOnExit()
    }

    def getSubDiredtoryContainingGivenFile(def path, def fileName) {
        def currentDir = new File(path)
        script.echo("canRead # " + currentDir.canRead())
        def dirPath = ""
        if(checkIfRequiredFileIsPresentInCurrentDir(currentDir,fileName)) {
            dirPath = currentDir.absolutePath
        }
        File[] children = currentDir.listFiles();
        if(dirPath == "") {
            for(def i=0;i < children.size(); i++) {
                if(children[i].isDirectory() && !children[i].name.contains(".")) {
                    if(checkIfRequiredFileIsPresentInCurrentDir(children[i],fileName)) {
                        dirPath = children[i].absolutePath
                        break
                    }
                }
            }
        }
        if(dirPath == "") {
            for(def i=0;i < children.size(); i++) {
                if(children[i].isDirectory() && !children[i].name.contains(".")) {
                    File[] subChildren = children[i].listFiles();
                    for(def j=0;j <subChildren.size();j++) {
                        if(subChildren[j].isDirectory() && !subChildren[j].name.contains(".")) {
                            if(checkIfRequiredFileIsPresentInCurrentDir(subChildren[j],fileName)) {
                                dirPath = subChildren[j].absolutePath
                                break
                            }
                        }
                    }                   
                }
                if(dirPath != "") {
                    break
                }
            }
        }
        return dirPath
    }
    
    def isGitUsed(def scm) {
        def gitUsed = true
        if(scm.class.toString().contains("SubversionSCM")){
            gitUsed = false
        }
        return gitUsed
    }

    def getLOBNameBasedOnGitRepo(def scmVars){
        return scmVars.GIT_URL.toUpperCase().contains("TNTLGSCR") ? "CARS" : scmVars.GIT_URL.toUpperCase().contains("TNTLGSCS") ? "CRUISE" : scmVars.GIT_URL.toUpperCase().contains("TNTLGS/") ? "LGS" : "HOTELS"
    }

    def getNexusGroupIdBasedOnGitRepo(def scmVars){
        def lob = getLOBNameBasedOnGitRepo(scmVars)
        if(lob.equalsIgnoreCase("CARS")){
            return "com.sabre.lgs.deployment.bundles.cars"
        }else if(lob.equalsIgnoreCase("CRUIS")){
            return "com.sabre.lgs.deployment.bundles.cruise"
        } else if(lob.equalsIgnoreCase("LGS")){
            return "com.sabre.lgs.deployment.bundles"
        } else {
            return "com.sabre.lgs.deployment.bundles.hotels"
        }
    }
    
    def getCurrentSCMRevision(def scm, def scmVars) {
        def scmRevision = ""
        if(scm.class.toString().contains("SubversionSCM")){
            scmRevision = scmVars.SVN_REVISION
        } else {
            scmRevision = scmVars.GIT_COMMIT
        }
        return scmRevision
    }
    
}