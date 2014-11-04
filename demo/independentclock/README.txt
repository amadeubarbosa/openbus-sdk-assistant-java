A demo IndependentClock tenta demonstrar um serviço de relógio que pode 
funcionar tanto conectado a um barramento, como de forma independente. O 
servidor imprime constantemente na tela o horário local. Em outra thread, tenta 
se conectar ao barramento, realizar o login e registrar sua oferta. Caso o login 
seja perdido, sua callback de login inválido tenta refazer esse processo 
eternamente até conseguir, mas a outra thread independente do barramento, que 
imprime a hora constantemente, continua funcionando.

O cliente, que também pode funcionar sem estar conectado ao barramento, imprime 
constantemente na tela a hora local. Em outra thread, tenta acessar o barramento 
para buscar e utilizar o servidor. Se conseguir, a thread de impressão de hora 
passa a imprimir na tela a hora do servidor. Caso a conexão com o barramento 
seja perdida, volta a imprimir a hora local até que consiga se reconectar ao 
barramento.

------------------------------
-------- DEPENDÊNCIAS---------
------------------------------

As dependências de software são fornecidas já compiladas, em conjunto com a demo.

ant-1.8.2.jar
ant-launcher-1.8.2.jar
jacorb-3.5.jar
jacorb-omgapi-3.5.jar
openbus-sdk-core-2.0-SNAPSHOT.jar
openbus-sdk-demo-util-2.0-SNAPSHOT.jar
openbus-sdk-legacy-2.0-SNAPSHOT.jar
openbus-sdk-assistant-core-2.0.0-SNAPSHOT.jar
scs-core-1.2.1.3.jar
slf4j-api-1.7.6.jar
slf4j-jdk14-1.7.6.jar


------------------------------
--------- ARGUMENTOS ---------
------------------------------

Servidor
1) host do barramento
2) porta do barramento
3) nome de entidade
4) caminho para a chave privada
5) tempo de espera entre cada tentativa de acesso ao barramento (em milisegundos 
e opcional - se não for fornecido, será 1)

Cliente
1) host do barramento
2) porta do barramento
3) nome de entidade
4) senha
5) tempo de espera entre cada tentativa de acesso ao barramento (em milisegundos 
e opcional - se não for fornecido, será 1)


------------------------------
---------- EXECUÇÃO ----------
------------------------------

Para que a demo funcione, pode ser necessário que as devidas permissões sejam 
cadastradas no barramento. Consulte o administrador do barramento e altere o 
arquivo admin\demo_independent_clock.adm conforme necessário.

A demo deve ser executada na seguinte ordem:

1) Servidor
2) Cliente


-------------------------------
----------- EXEMPLO -----------
-------------------------------
Supondo que os jars que o demo depende estão em um diretório chamado 'lib':

1) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-independentclock-2.0-SNAPSHOT.jar demo.IndependentClockServer localhost 2089 demo_independentclock_java DemoIndependentClock.key

2) java -Djava.endorsed.dirs=./lib/ -cp $(echo lib/*.jar | tr ' ' ':'):openbus-sdk-demo-independentclock-2.0-SNAPSHOT.jar demo.IndependentClockClient localhost 2089 demo_independentclock_java_client minhasenha
