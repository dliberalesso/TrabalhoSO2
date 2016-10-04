package me.dliberalesso;

import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        // define opções
        Options options = new Options();
        options.addOption(Option.builder("m")
                .desc("inicia no modo (G)erenciador de processos ou "
                        + "inicia no modo (H)host e simula core(s) da CPU")
                .required(true)
                .hasArg(true)
                .argName("MODO")
                .build()
        );
        options.addOption(Option.builder("ip")
                .desc("indica o ENDEREÇO IP para onde deve conectar")
                .hasArg(true)
                .argName("127.0.0.1")
                .build()
        );
        options.addOption(Option.builder("p")
                .desc("indica a PORTA por onde deve conectar")
                .hasArg(true)
                .argName("54321")
                .build()
        );
        options.addOption(Option.builder("c")
                .desc("indica por quantos CORES/CONEXÕES deve esperar")
                .hasArg(true)
                .argName("4")
                .build()
        );

        // faz o parsing da linha de comando
        CommandLineParser parser = new DefaultParser();
        CommandLine line;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("TrabalhoSO2", options, true);
            return;
        }

        // valida quantidade de CORES/CONEXOES
        int cores_conexoes = 4;
        try {
            if (line.hasOption("c")) cores_conexoes = Integer.parseInt(line.getOptionValue("c"));
            if (cores_conexoes <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Valor informado deve ser um inteiro maior que 0!");
            new HelpFormatter().printHelp("TrabalhoSO2", options, true);
            return;
        }

        // valida PORTA
        int porta = 54321;
        try {
            if (line.hasOption("p")) porta = Integer.parseInt(line.getOptionValue("p"));
            if (porta < 1024 || porta > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("PORTA deve ser um inteiro entre 1024...65535!");
            new HelpFormatter().printHelp("TrabalhoSO2", options, true);
            return;
        }

        // valida ENDEREÇO IP
        InetAddress endereco;
        try {
            if (line.hasOption("ip")) {
                endereco = InetAddress.getByName(line.getOptionValue("ip"));
            } else {
                // endereco = InetAddress.getLocalHost(); // retorna IP externo?
                endereco = InetAddress.getByName("localhost"); // retorna IP interno?
            }
        } catch (UnknownHostException e) {
            System.out.println("Certifique-se de que o ENDEREÇO IP informado esteja correto!");
            new HelpFormatter().printHelp("TrabalhoSO2", options, true);
            return;
        }

        // valida MODO
        try {
            String modo = line.getOptionValue("m");
            if (modo.equals("G")) {
                Gerenciador.start();
            } else if (modo.equals("H")) {
                System.out.println("Ainda não temos um HOST");
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            System.out.println("Modo deve ser 'G' ou 'H'!");
            new HelpFormatter().printHelp("TrabalhoSO2", options, true);
        }
    }
}
