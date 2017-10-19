import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.javatuples.Triplet;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.Parts;
import krpc.client.services.SpaceCenter.Leg;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.VesselSituation;
import krpc.client.services.UI;
import krpc.client.services.UI.Canvas;
import krpc.client.services.UI.Panel;
import krpc.client.services.UI.RectTransform;
import krpc.client.services.UI.Text;

public class SuicideBurn {

	// Streams de conex�o - Declara��o:

	private static Connection conexao;
	private static SpaceCenter centroEspacial;
	private static SpaceCenter.Vessel naveAtual;
	private static Stream<Double> ut;
	private static ReferenceFrame pontoReferencia;
	private static ReferenceFrame refVelocidade;
	private static Flight parametrosVoo;
	private static Stream<Double> altitudeSuperficie;
	private static Stream<Float> massaTotalNave;
	private static Stream<Float> massaSecaNave;
	private static Triplet<Double, Double, Double> velocidade;
	private static PrintStream gravadorDeDados;

	// Vari�veis de controles:
	private static double tempoAteImpacto;
	private static double velocidadeNoImpacto;
	private static double acelMax;
	private static float aceleracaoGravidade;
	private static float naveTWR;
	private static double tempoDeQueima;
	private static double taxaDeQueima;
	private static double distanciaDaQueima;
	private static double naveDeltaV;
	private static float massaMedia;
	private static float aceleracao = 1f;
	private static double correcaoAceleracao = 1.0;
	private static boolean suicideInicio = false;
	private static String dadosColetados = null;
	private static VesselSituation pouso;
	static int amostras = 5;

	public static void main(String[] args) throws IOException, RPCException, InterruptedException, StreamException {

		gravadorDeDados = new PrintStream("valores.txt");
		// Inicializa��o das vari�veis est�ticas:
		conexao = Connection.newInstance("Suicide Burn - Teste"); // indica uma nova conex�o com o kRPC
		centroEspacial = SpaceCenter.newInstance(conexao); // cria uma nova inst�ncia do centro de espacial com a
															// conex�o
		naveAtual = centroEspacial.getActiveVessel(); // cria uma nova inst�ncia nave � partir do centro
		centroEspacial.getUT(); // Pega o tempo universal pelo centro espacial
		ut = conexao.addStream(SpaceCenter.class, "getUT"); // Adiciona o "Tempo Universal" do jogo � Stream de tempo.
		pontoReferencia = naveAtual.getOrbitalReferenceFrame(); // Ponto de refer�ncia orbital da nave
		refVelocidade = ReferenceFrame.createHybrid(conexao, // Ponto de refer�ncia da velocidade de superf�cie em
																// rela��o ao planeta
				naveAtual.getOrbit().getBody().getReferenceFrame(), naveAtual.getSurfaceReferenceFrame(),
				naveAtual.getOrbit().getBody().getReferenceFrame(), naveAtual.getOrbit().getBody().getReferenceFrame());

		parametrosVoo = naveAtual.flight(pontoReferencia); // Adiciona o ponto de refer�ncia da nave � Stream de
															// parametros de voo
		altitudeSuperficie = conexao.addStream(parametrosVoo, "getSurfaceAltitude"); // altitude acima da superf�cie
		massaTotalNave = conexao.addStream(naveAtual, "getMass"); // Adiciona a Stream de massa total da nave
		massaSecaNave = conexao.addStream(naveAtual, "getDryMass"); // Adiciona a Stream de massa seca da nave
		velocidade = naveAtual.flight(refVelocidade).getVelocity(); // Adiciona a Stream de velocidade da nave
		aceleracaoGravidade = naveAtual.getOrbit().getBody().getSurfaceGravity(); // acelera��o da gravidade do corpo
																					// celeste orbitado ao n�vel do mar

		// ---------- C�DIGO DE EXECU��O E CHECAGEM DO SUICIDE BURN! ---------------

		while (true) {
			atualizarVariaveis();
			System.out.println("Teste Suicide: 5 < " + (altitudeSuperficie.get()
					- velocidade.getValue0() * tempoDeQueima + 1 / 2 * acelMax * (tempoDeQueima * tempoDeQueima)));

			if (checarSuicide()) {
				System.out.println("�nicio do Suicide Burn!!!");
				suicideInicio = true;
				naveAtual.getControl().setThrottle(0.9f);
				aceleracao = 1f;
				correcaoAceleracao = 1.0;
				break;
			}
		}

		while (suicideInicio) {
			atualizarVariaveis();

			System.out.println("Aceleracao: " + aceleracao);
			System.out.println("Delta V : " + naveDeltaV);
			System.out.println("Taxa De Queima: " + taxaDeQueima);
			System.out.println("Nave TWR: " + naveTWR);
			System.out.println("Corre��o Acel: " + correcaoAceleracao);
			System.out.println("Tempo Queima: " + tempoDeQueima);

			if (!checarSuicide()) {
				// aceleracao = naveAtual.getControl().getThrottle();
				atualizarVariaveis();
				System.out.println("Ap�s corre��o: " + aceleracao);
				naveAtual.getControl().setThrottle(aceleracao);

				// Corre��o mais correta at� agora:
				/*
				 * correcaoAceleracao = Math.abs(Math.log(Math.sqrt(aceleracao / (naveDeltaV *
				 * taxaDeQueima) * naveTWR) / Math.abs(Math.sqrt(Math.abs(tempoDeQueima /
				 * 2)))));
				 * 
				 * melhor correcaoAceleracao = Math.abs(Math.log(Math.sqrt(1 / (naveDeltaV *
				 * taxaDeQueima) * naveTWR) / Math.abs(Math.sqrt(Math.abs(tempoDeQueima /
				 * naveTWR)))));
				 * 
				 * melhor ainda correcaoAceleracao = Math.abs(Math.log(Math.sqrt(1 / (naveDeltaV
				 * * taxaDeQueima) * naveTWR) / Math.abs(Math.sqrt(Math.abs(tempoDeQueima /
				 * (naveTWR*aceleracao))))));
				 * 
				 * definitiva? correcaoAceleracao = Math.abs(Math.log(Math.sqrt(aceleracao /
				 * (naveDeltaV * taxaDeQueima / naveTWR) /
				 * Math.abs(Math.sqrt(Math.abs(tempoDeQueima / 2))))));
				 * 
				 * talvez a final
				 * correcaoAceleracao = Math.abs(Math.log(Math.sqrt
							(aceleracao / ((naveDeltaV * Math.abs(tempoDeQueima / 2)) * (taxaDeQueima) / naveTWR))
							/ Math.abs(Math.sqrt(Math.abs(tempoDeQueima / 2)))));
				 */
			}

			if (altitudeSuperficie.get() < 300) {
				List<Leg> pernas = naveAtual.getParts().getLegs();
				for (Leg perna : pernas) {
					if (perna.getDeployable() && !perna.getDeployed()) {
						perna.setDeployed(true);
					}
				}
			}

			// CHECK-UP DE VELOCIDADE PARA 95% DE TWR

			if (velocidade.getValue0() > -8.0) {
				
					atualizarVariaveis();
					if (velocidade.getValue0() > -8.0) {
						aceleracao = (float) (correcaoAceleracao * 0.95 / naveTWR);
						naveAtual.getControl().setThrottle(aceleracao);
					}
					
					pouso = naveAtual.getSituation();
					if (pouso.toString() == "LANDED") {
						System.out.println("Pouso finalizado!");
						suicideInicio = false;
						naveAtual.getControl().setThrottle(0);
						gravadorDeDados.close();
						conexao.close();
						break;
					}
				
			}

			pouso = naveAtual.getSituation();
			if (pouso.toString() == "LANDED") {
				System.out.println("Pouso finalizado!");
				suicideInicio = false;
				naveAtual.getControl().setThrottle(0);
				gravadorDeDados.close();
				conexao.close();
				break;
			}
		}

	}

	public static void atualizarVariaveis() throws InterruptedException {
		try {
			tempoAteImpacto = Math.sqrt(2 * altitudeSuperficie.get() * 1 / aceleracaoGravidade);
			velocidade = naveAtual.flight(refVelocidade).getVelocity();
			velocidadeNoImpacto = velocidade.getValue0() + aceleracaoGravidade * (tempoAteImpacto * tempoAteImpacto);
			distanciaDaQueima = -velocidade.getValue0() * tempoDeQueima + 1 / 2 * acelMax * Math.pow(tempoDeQueima, 2);
			naveDeltaV = Math.log(massaTotalNave.get() / massaSecaNave.get()) * naveAtual.getSpecificImpulse()
					* aceleracaoGravidade;

			massaMedia = (float) ((massaTotalNave.get() + (massaTotalNave.get()
					/ Math.pow(Math.E, (naveDeltaV / (naveAtual.getSpecificImpulse() * aceleracaoGravidade))))) / 2);

			naveTWR = naveAtual.getAvailableThrust() / (massaTotalNave.get() * aceleracaoGravidade);
			taxaDeQueima = naveTWR / naveAtual.getSpecificImpulse();
			acelMax = (naveTWR * aceleracaoGravidade) - aceleracaoGravidade;
			tempoDeQueima = velocidade.getValue0() / acelMax;

			// -=-=-=- LINHA MUITO IMPORTANTE, DETERMINA A CORRE��O PARA ACELERA��O AO
			// POUSAR -=-=-=-
			correcaoAceleracao = Math.abs(Math.log(Math.abs(Math.sqrt(Math.abs
					(aceleracao / ((naveDeltaV * Math.abs(tempoDeQueima)) * (taxaDeQueima) / naveTWR))))));

			// gravar log:
			dadosColetados = coletarDados(aceleracao, taxaDeQueima, naveDeltaV, naveTWR, tempoDeQueima);
			if (dadosColetados != null) {
				gravadorDeDados.println(dadosColetados);
			}

			aceleracao = (float) correcaoAceleracao;
			Thread.sleep(25);

		} catch (IOException e) {
			System.out.println("Erro de grava��o de dados: " + e.getMessage());
		} catch (RPCException e) {
			System.out.println("Erro de conex�o com o Mod: " + e.getMessage());
		} catch (StreamException e) {
			System.out.println("Streams n�o iniciadas");
			e.printStackTrace();
		}
	}

	public static boolean checarSuicide() {
		try {
			if (0 >= altitudeSuperficie.get() - velocidade.getValue0() * tempoDeQueima
					+ 1 / 2 * acelMax * (tempoDeQueima * tempoDeQueima)) {
				return true;
			}
		} catch (StreamException | RPCException | IOException e) {
			System.out.println("Erro ao checar hora correta do Suicide Burn");
			e.printStackTrace();
		}
		return false;
	}

	public static String coletarDados(double dados1, double dados2, double dados3, double dados4, double dados5) {
		String resultado = null;
		if (amostras > 0) {
			amostras--;
		} else {
			resultado = String.valueOf(dados1) + "," + String.valueOf(dados2) + "," + String.valueOf(dados3) + ","
					+ String.valueOf(dados4) + "," + String.valueOf(dados5) + ",";
			amostras = 5;
		}
		return resultado;
	}

}
