package me.dliberalesso;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TrabalhoSO2 {
    private static Options options = new Options();

    public static void main(String[] args) {
        // define opções
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

        // TODO ver uma forma alternativa a esse monte de try/catch
        // faz o parsing da linha de comando
        CommandLineParser parser = new DefaultParser();
        CommandLine line;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            ajuda(e.getMessage());
            return;
        }

        // valida quantidade de CORES/CONEXOES
        int poolSize = 4;
        try {
            if (line.hasOption("c")) poolSize = Integer.parseInt(line.getOptionValue("c"));
            if (poolSize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ajuda("Valor informado deve ser um inteiro maior que 0!");
            return;
        }

        // valida PORTA
        int porta = 54321;
        try {
            if (line.hasOption("p")) porta = Integer.parseInt(line.getOptionValue("p"));
            if (porta < 1024 || porta > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ajuda("PORTA deve ser um inteiro entre 1024...65535!");
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
            ajuda("Certifique-se de que o ENDEREÇO IP informado esteja correto!");
            return;
        }

        // valida MODO
        try {
            String modo = line.getOptionValue("m");
            if (modo.equals("G")) {
                new Gerenciador(porta, poolSize).run();
            } else if (modo.equals("H")) {
                new Host(endereco, porta, poolSize).run();
            } else {
                throw new Exception();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            ajuda("Modo deve ser 'G' ou 'H'!");
        }
    }

    private static void ajuda(String mensagem) {
        System.out.println(mensagem + "\n");
        new HelpFormatter().printHelp("TrabalhoSO2", "", options,
                "\nAlunos: Douglas Liberalesso, Rafahel Mello, Tayrone Rockenbach.", true);
    }
}
