import krpc.client.RPCException;
import krpc.client.StreamException;

public class ControlePID {

	private static double ultCalculo = 0; 	//tempo do �ltimo c�lculo
	private static double valorEntrada, valorSaida, valorLimite; //vari�veis de valores
	private static double termoInt, ultValorEntrada; // vari�veis de c�lculo de erro
	private static double kp, ki, kd; // variaveis de ajuste do PID
	private static double amostraTempo = 0.020; // tempo de amostragem
	private static double saidaMin, saidaMax;

	//-=- C�digo principal, para computar o PID -=-
	public double computarPID() throws RPCException, StreamException {
		double agora = SuicideBurn.UT.get(); // vari�vel que busca o tempo imediato
		double mudancaTempo = agora - ultCalculo; // vari�vel que compara o tempo de c�lculo

		if (mudancaTempo >= amostraTempo) { //se a mudan�a for maior que o tempo de amostra, o c�lculo � feito.
			//vari�veis para o c�lculo do valor de sa�da
			double erro = valorLimite - valorEntrada;
			termoInt += ki * erro;
			if (termoInt >saidaMax) {termoInt = saidaMax;}
			else if (termoInt < saidaMin) {termoInt = saidaMin;}
			double dvalorEntrada = (valorEntrada - ultValorEntrada);
			
			//computando o valor de sa�da
			valorSaida = kp * erro + ki * termoInt - kd * dvalorEntrada;
			if (valorSaida > saidaMax) {valorSaida = saidaMax;}
			else if (valorSaida < saidaMin) {valorSaida = saidaMin;}
			
			//relembrando os valores atuais para a pr�xima vez
			ultValorEntrada = valorEntrada;
			ultCalculo = agora;
		}
		//retornando o valor para quem chamou esse m�todo
		return ControlePID.valorSaida;
	}

	public void setValorEntrada(double valor) {
		if (valor > 0) {
			ControlePID.valorEntrada = valor;
		}
	}

	public void setValorLimite(double valor) {
		if (valor > 0) {
			ControlePID.valorLimite = valor;
		}
	}

	public void setLimiteSaida( double Min, double Max) {
		if (Min > Max) return;
		saidaMin = Min;
		saidaMax = Max;

		if (termoInt >saidaMax) {termoInt = saidaMax;}
		else if (termoInt < saidaMin) {termoInt = saidaMin;}

		if (valorSaida > saidaMax) {valorSaida = saidaMax;}
		else if (valorSaida < saidaMin) {valorSaida = saidaMin;}
	}

	public void setAjustes(double Kp, double Ki, double Kd) {
		kp = Kp;
		ki = Ki;
		kd = Kd;
	}

	public void setAmostraTempo(double novaAmostraTempo) {
		if (novaAmostraTempo > 0) {
			amostraTempo = novaAmostraTempo/1000;
		}
	}
}
