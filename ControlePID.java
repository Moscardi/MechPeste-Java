import krpc.client.RPCException;
import krpc.client.StreamException;

public class ControlePID {

	private double valorEntrada, valorSaida, valorLimite; //vari�veis de valores
	private double termoInt, ultValorEntrada; // vari�veis de c�lculo de erro
	private double kp, ki, kd; // variaveis de ajuste do PID
	private double ultCalculo = 0; 	//tempo do �ltimo c�lculo
	private double amostraTempo = 0.020; // tempo de amostragem
	private double saidaMin, saidaMax;

	//-=- C�digo principal, para computar o PID -=-
	public double computarPID() throws RPCException, StreamException {
		double agora = SuicideBurn.tempoUniversal.get(); // vari�vel que busca o tempo imediato
		double mudancaTempo = agora - this.ultCalculo; // vari�vel que compara o tempo de c�lculo

		if (mudancaTempo >= this.amostraTempo) { //se a mudan�a for maior que o tempo de amostra, o c�lculo � feito.
			//vari�veis para o c�lculo do valor de sa�da
			double erro = this.valorLimite - this.valorEntrada;
			termoInt += ki * erro;
			if (termoInt >saidaMax) {termoInt = saidaMax;}
			else if (termoInt < saidaMin) {termoInt = saidaMin;}
			double dvalorEntrada = (this.valorEntrada - ultValorEntrada);
			
			//computando o valor de sa�da
			this.valorSaida = kp * erro + ki * termoInt - kd * dvalorEntrada;
			if (this.valorSaida > saidaMax) {this.valorSaida = saidaMax;}
			else if (this.valorSaida < saidaMin) {this.valorSaida = saidaMin;}
			
			//relembrando os valores atuais para a pr�xima vez
			ultValorEntrada = this.valorEntrada;
			this.ultCalculo = agora;
		}
		//retornando o valor para quem chamou esse m�todo
		return this.valorSaida;
	}

	public void setValorEntrada(double valor) {
		if (valor > 0) {
			this.valorEntrada = valor;
		}
	}
	public void setValorLimite(double valor) {
		if (valor > 0) {
			this.valorLimite = valor;
		}
	}
	public void setLimiteSaida( double Min, double Max) {
		if (Min > Max) return;
		saidaMin = Min;
		saidaMax = Max;

		if (termoInt >saidaMax) {termoInt = saidaMax;}
		else if (termoInt < saidaMin) {termoInt = saidaMin;}

		if (this.valorSaida > saidaMax) {this.valorSaida = saidaMax;}
		else if (this.valorSaida < saidaMin) {this.valorSaida = saidaMin;}
	}
	public void setAjustes(double Kp, double Ki, double Kd) {
		kp = Kp;
		ki = Ki;
		kd = Kd;
	}
	public void setAmostraTempo(double novaAmostraTempo) {
		if (novaAmostraTempo > 0) {
			this.amostraTempo = novaAmostraTempo/1000;
		}
	}
}
