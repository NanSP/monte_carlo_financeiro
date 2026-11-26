# Ciclo de vida das tarefas

`JobCoordinator` limita simultaneamente as tarefas em execução e as tarefas
na fila. `submit(parâmetros, motor)` devolve um UUID; se a fila estiver cheia,
lança `RejectedExecutionException` sem registrar uma tarefa rejeitada.

Estados: `QUEUED → RUNNING → SUCCEEDED` ou `FAILED`. Uma tarefa em fila ou
execução também pode passar para `CANCELLED`. Estados finais não mudam.
`snapshot(id)` devolve uma visão imutável com progresso, estimativa parcial,
resultado ou erro. `await(id, prazo, unidade)` espera sem sondagem ativa;
`cancel(id)` interrompe a execução e libera imediatamente um lugar na fila
quando a tarefa ainda não começou. `forget(id)` libera o registro de uma
tarefa finalizada depois que o cliente coletar seus dados.

Os motores sequencial e com threads publicam progresso ao concluir blocos.
A estimativa parcial do motor com threads combina blocos na ordem em que
terminam; por isso ela pode variar nos últimos bits entre execuções. O
resultado final continua combinando blocos por índice e permanece igual ao
resultado sequencial.

O coordenador não abre portas de rede. A interface RMI e o canal TCP serão
adicionados sobre essa API na próxima etapa.
