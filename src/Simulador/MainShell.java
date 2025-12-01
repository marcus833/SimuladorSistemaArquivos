package Simulador;

import java.io.*;
import java.util.*;

public class MainShell {
    public static void main(String[] args) throws Exception {
        String image = "fs.img";
        String journal = "fs.journal";
        FileSystemSimulator fs = new FileSystemSimulator(image, journal);
        System.out.println("Simulador de Sistema de Arquivos (shell). Digite 'help' para ver comandos.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" ", 3);
            String cmd = parts[0];
            try {
                switch (cmd) {
                    case "help":
                        System.out.println("comandos: ls, mkdir, rmdir, touch, write, cat, rm, mv, cp, exit");
                        break;
                    case "ls":
                        String p = parts.length>1 ? parts[1] : "/";
                        System.out.println(fs.ls(p));
                        break;
                    case "mkdir":
                        fs.mkdir(parts[1]); System.out.println("ok");
                        break;
                    case "rmdir":
                        fs.rmdir(parts[1]); System.out.println("ok");
                        break;
                    case "touch":
                        fs.touch(parts[1]); System.out.println("ok");
                        break;
                    case "write":
                        fs.writeFile(parts[1], parts.length>2 ? parts[2] : "");
                        System.out.println("ok");
                        break;
                    case "cat":
                        System.out.println(fs.readFile(parts[1]));
                        break;
                    case "rm":
                        fs.rm(parts[1]); System.out.println("ok");
                        break;
                    case "mv":
                        fs.mv(parts[1], parts[2]); System.out.println("ok");
                        break;
                    case "cp":
                        fs.cp(parts[1], parts[2]); System.out.println("ok");
                        break;
                    case "exit":
                        System.out.println("Saindo.");
                        return;
                    default:
                        System.out.println("Comando desconhecido");
                }
            } catch (Exception ex) {
                System.out.println("ERRO: " + ex.getMessage());
            }
        }
    }
}
