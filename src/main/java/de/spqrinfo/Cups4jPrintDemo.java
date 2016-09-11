package de.spqrinfo;

import de.spqrinfo.cups4j.*;

import java.io.InputStream;
import java.net.URL;

public class Cups4jPrintDemo {
    public static void main(final String... args) throws Exception {
        System.out.println("PrintDemo using cups4j");

        final String printServerName = "192.169.173.7";

        final CupsClient client = new CupsClient(printServerName, CupsClient.DEFAULT_PORT);

        Exception savedException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            savedException = null;
            try {
                client.getPrinters().forEach(System.out::println);
            } catch (final Exception ex) {
                savedException = ex;
                System.err.println("Failed getting printers, attempt " + attempt);
                continue;
            }
            break;
        }

        if (savedException != null) {
            System.err.println("Failed getting printers: " + savedException.getMessage());
            savedException.printStackTrace(System.err);
        }

        final String printerName = args[0];
        final CupsPrinter printer = new CupsPrinter(new URL("http://" + printServerName + ":631/printers/" + printerName), printerName, true);

        // Test simple text printing
        try (final InputStream in = Cups4jPrintDemo.class.getResourceAsStream("lorem.txt")) {
            if (in == null) {
                throw new RuntimeException("Can't get sample resource to print");
            }

            final PrintJob printJob = new PrintJob.Builder(in).jobName("lorem").build();
            analyzePrintJob(client, printer, printJob);
        }

        // Test duplex pdf printing
        try (final InputStream in = Cups4jPrintDemo.class.getResourceAsStream("sample.pdf")) {
            if (in == null) {
                throw new RuntimeException("Can't get sample resource to print");
            }

            final PrintJob duplexPrintJob = new PrintJob.Builder(in)
                    .jobName("lorem")
                    .duplex(true)
                    .pageRanges("5-6")
                    .build();

            analyzePrintJob(client, printer, duplexPrintJob);
        }

        // Test simplex pdf printing
        try (final InputStream in = Cups4jPrintDemo.class.getResourceAsStream("sample.pdf")) {
            if (in == null) {
                throw new RuntimeException("Can't get sample resource to print");
            }

            final PrintJob duplexPrintJob = new PrintJob.Builder(in)
                    .jobName("lorem")
                    .duplex(false)
                    .pageRanges("5-6")
                    .build();

            analyzePrintJob(client, printer, duplexPrintJob);
        }
    }

    private static void analyzePrintJob(CupsClient client, CupsPrinter printer, PrintJob printJob) throws Exception {
        final PrintRequestResult printRequestResult = printer.print(printJob);
        if (printRequestResult.isSuccessfulResult()) {
            final int jobID = printRequestResult.getJobId();

            System.out.println("file sent to " + printer.getPrinterURL() + " jobID: " + jobID);
            System.out.println("... current status = " + printer.getJobStatus(jobID));
            Thread.sleep(1000);
            System.out.println("... status after 1 sec. = " + printer.getJobStatus(jobID));

            System.out.println("Get last PrintJob");
            final PrintJobAttributes job = client.getJobAttributes("localhost", jobID);
            System.out.println("ID: " + job.getJobID() + " user: " + job.getUserName() + " url: " + job.getJobURL() + " status: "
                    + job.getJobState());

        } else {
            System.out.println("Print error! status code: " + printRequestResult.getResultCode() + " status description: "
                    + printRequestResult.getResultDescription());
        }
    }
}
