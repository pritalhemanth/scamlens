package com.scamlens;

import com.scamlens.analyzer.MessageAnalyzer;
import com.scamlens.service.HistoryService;
import com.scamlens.web.WebServer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Starts the ScamLens website.
 *
 *   1. creates the detection engine (MessageAnalyzer)
 *   2. opens the history file (HistoryService)
 *   3. starts the web server and prints the address to open
 *
 * The port is 8765 on your laptop. Hosting sites such as Render tell the app
 * which port to use through the PORT environment variable.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // 8765 is rarely used by other programs (8080 is often taken, e.g. by Steam)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8765"));

        Path publicFolder = Path.of("public");
        if (!Files.isDirectory(publicFolder)) {
            System.err.println("Could not find the 'public' folder. Start the app from the main scamlens-website folder.");
            System.exit(1);
        }

        MessageAnalyzer analyzer = new MessageAnalyzer();
        HistoryService history = new HistoryService(HistoryService.defaultFile());

        WebServer server = new WebServer(analyzer, history, publicFolder);
        try {
            server.start(port);
        } catch (java.net.BindException portInUse) {
            System.err.println("Port " + port + " is already being used by another program.");
            System.err.println("Close that program, or start ScamLens on another port, for example in PowerShell:");
            System.err.println("    $env:PORT=9090; .\\run.bat");
            System.exit(1);
        }

        System.out.println("ScamLens is running.");
        System.out.println("Open this in your browser:  http://localhost:" + port);
        System.out.println("Press Ctrl + C to stop.");
    }
}
