package client;
// Java implementation for a client 

// Save file as Client.java 

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;

import crypto.AsymmetricCryptoManager;
import crypto.SymmetricCryptoManager;
import server.Mensagem;

// Client class 
public class Client {
	private static long id = 1; //TODO: Passar user E senha pro server?

	//public static final byte ENVIA_ARQ = (byte) 0x00;
	public static final byte RECEBE_ARQ = (byte) 0x03;
	public static final byte ENVIA_REQ = (byte) 0x04;
	public static final byte ENVIA_ARQ_REP_CLIENT = (byte) 0x06;
	public static final byte ENVIA_ARQ_DIV_CLIENT = (byte) 0x07;
	
	public static void main(String[] args) throws IOException {
		try {
			Scanner scn = new Scanner(System.in);
			String[] argumentos = new String[4];
			SymmetricCryptoManager sCryptoManager = new SymmetricCryptoManager();

			//Arquivo de configuracao (argumentos)
			BufferedReader bfr = new BufferedReader( new FileReader("clientConf.txt"));
			String line = bfr.readLine();
			bfr.close();
			line = line.replaceAll(" ", "");//Remove todos os espacos
			String[] split = new String[4];
			split = line.split(",");
			argumentos[0] = split[0];
			argumentos[1] = split[1];
			argumentos[2] = split[2];
			argumentos[3] = split[3];
			

			// running infinite loop for getting client request
			int sPort = Integer.parseInt(argumentos[1]);
			int cPort = Integer.parseInt(argumentos[3]);
			
	        InetAddress sIP = InetAddress.getByName(argumentos[0]);
	        InetAddress cIP = InetAddress.getByName(argumentos[2]);
	    	
			// establish the connection
			Socket connection = new Socket(sIP, sPort, cIP, cPort);

			// obtaining input and out streams
			DataInputStream dis = new DataInputStream(connection.getInputStream());
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			
			// Starts keys exchange
			byte[] ServerPublicKey = Base64.getDecoder().decode(dis.readUTF());
			byte[] clientEncodedKey =  sCryptoManager.getKey().getEncoded();
			byte[] encryptedSymmetricKey = AsymmetricCryptoManager.encryptData(clientEncodedKey, ServerPublicKey);
			dos.writeUTF(Base64.getEncoder().encodeToString(encryptedSymmetricKey));
						
			// Test sending encrypted data
			String test = "troca de chave realizada com sucesso";
			byte[] testBytes = sCryptoManager.encryptData(test.getBytes());
			dos.writeUTF(Base64.getEncoder().encodeToString(testBytes));
			
			// Thread
			Thread t = new ServerHandler(connection, dis, dos, sCryptoManager);
			t.setName(cIP.getHostName());
			t.start();
			// System.out.println("Iniciando Thread para recebimento de arquivos");
			
			boolean loop = true;
			while (loop) {

				System.out.println("===SELECAO DE OPCAO===");
				//System.out.println("Digite 'upload' para enviar um arquivo.");
				System.out.println("Digite 'replicacao' para enviar e replicar o arquivo nos servidor de armazenamento.");
				System.out.println("Digite 'divisao' para enviar e dividir o arquivo entre os servidor de armazenamento.");
				System.out.println("Digite 'download' para baixar um arquivo.");
				System.out.println("Digite 'sair' para sair.");
				String opcao = scn.nextLine();
				byte[] bytes = null;
				String nomeArq = null;
				Path pathArq = null;
				switch (opcao) {
//				case "upload":
//					pathArq = null;
//					modo = ENVIA_ARQ;
//					System.out.println("Escreva o nome do arquivo a ser enviado: ");
//					pathArq = Paths.get(scn.nextLine());
//					nomeArq = pathArq.getFileName().toString();
//					bytes = getArq(pathArq.toString());
//					break;
				case "replicacao":
					pathArq = null;

					System.out.println("Escreva o nome do arquivo a ser enviado: ");
					pathArq = Paths.get(scn.nextLine());
					nomeArq = pathArq.getFileName().toString();
					bytes = sCryptoManager.encryptData(getArq(pathArq.toString()));					
					byte[] outputPackageR = new Mensagem(ENVIA_ARQ_REP_CLIENT, id, nomeArq, bytes).getMessage();
					dos.write(outputPackageR);
					break;
				case "divisao":
					pathArq = null;
					System.out.println("Escreva o nome do arquivo a ser enviado: ");
					pathArq = Paths.get(scn.nextLine());
					nomeArq = pathArq.getFileName().toString();
					bytes = sCryptoManager.encryptData(getArq(pathArq.toString()));				
					byte[] outputPackageF = new Mensagem(ENVIA_ARQ_DIV_CLIENT, id, nomeArq, bytes).getMessage();
					dos.write(outputPackageF);
					break;
				case "download":
					System.out.println("Escreva o nome do arquivo a ser baixado: ");
					nomeArq = scn.nextLine();
					bytes = new byte[0];		
					byte[] outputPackageD = new Mensagem(ENVIA_REQ, id, nomeArq, bytes).getMessage();
					dos.write(outputPackageD);
					break;
				case "sair":
					System.out.println("Fechando conexao: " + connection);
					connection.close();
					System.out.println("Conexao fechada.");
					loop = false;
					break;
				default:
					System.out.println("Invalid input given by user! Try again!");
					break;
				}
			}
			// closing resources
			dis.close();
			dos.close();
			scn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// ============ METODOS UTILITARIOS =====================

	// Retorna os bytes[] de um long
	public static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	// Retorna os bytes[] de um int
	public static byte[] intToBytes(int i) {
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i);

		return result;
	}

	// File -> byte[]
	public static byte[] readContentIntoByteArray(File file) {
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try {
			// convert file into array of bytes
			if (file.exists()) {
				fileInputStream = new FileInputStream(file);
				fileInputStream.read(bFile);
				fileInputStream.close();
				// for (int i = 0; i < bFile.length; i++) {
//					System.out.print((char) bFile[i]);
				// }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bFile;
	}

	// Retorna os bytes[] do arquivo especificado 
	public static byte[] getArq(String nomeArq) {
		byte[] b;

		File testeArq = new File(nomeArq);
		b = readContentIntoByteArray(testeArq);

		return b;
	}

}

//>>>>>> THREAD - Para recebimento dos arquivos
class ServerHandler extends Thread {
	final DataInputStream dis;
	final DataOutputStream dos;
	final Socket s;
	final SymmetricCryptoManager sCryptoManager;

	// Constantes do cabecalho (modo)
	//public static final byte ENVIA_ARQ = (byte) 0x00;
	public static final byte RECEBE_ARQ = (byte) 0x03;
	public static final byte ENVIA_REQ = (byte) 0x04;

	public ServerHandler(Socket s, DataInputStream dis, DataOutputStream dos, SymmetricCryptoManager sCryptoManager) {
		this.s = s;
		this.dis = dis;
		this.dos = dos;
		this.sCryptoManager = sCryptoManager;
	}

	@Override
	public void run() {
		while (true) {
			try {
				// READING
				int lenght = dis.readInt();

				byte[] received = new byte[lenght];
				// Reconstroi o int lido
				byte[] lb = intToBytes(lenght);
				for (int i = 0; i < Integer.BYTES; i++) {
					received[i] = lb[i];
				}
				byte[] buffer = new byte[lenght - Integer.BYTES];
				dis.readFully(buffer);
				int c = 0;
				for (int i = Integer.BYTES; i < lenght; i++) {
					received[i] = buffer[c];
					c++;
				}
				c = 0;

				Mensagem msg = new Mensagem(received);
				byte mode = msg.getHeader().getMode();
				// byte[] bUser = msg.getHeader().getBUser();
				byte[] bNomeArq = msg.getHeader().getBNome();
				byte[] body = sCryptoManager.decryptData(msg.getBody());
				String nomeArq = new String(bNomeArq, StandardCharsets.UTF_8);
				String[] split = nomeArq.split("/");
				nomeArq = split[split.length-1]; //Pega so o nome do arquivo, sem diretorio

				if (mode == RECEBE_ARQ) {
					writeArq(body, nomeArq);
				}

			}catch (SocketException se) {
				se.printStackTrace();
				break;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Cria o arquivo

	
	public static void writeArq(byte[] arq, String _nomeArq) {
		System.out.println("CLIENT - Arquivo criado.");

		try (FileOutputStream stream = new FileOutputStream(_nomeArq)) {
			stream.write(arq);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static byte[] intToBytes(int i) {
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i);

		return result;
	}

}