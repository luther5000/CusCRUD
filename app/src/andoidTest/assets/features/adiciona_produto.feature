Funcionalidade: Adicionar novos produtos ao inventário
	Como usuário do sistema
	Quero adicionar um novo produto ao meu inventário
	Para guardar as informações dele

	Contexto:
		Dado que o usuário está na tela de adicionar produtos

	Cenário: Adição feita com sucesso
		Quando o usuário insere as informações relacionadas ao novo produto
		E clica no botão de adicionar
		Então o sistema informa o usuário que o produto foi adicionado com sucesso
		E o usuário retorna para a tela inicial do seu inventário
		E o novo produto passa a ser contabilizado na listagem

	Cenário: Cancelar a ação
		Quando o usuário insere as informações relacionadas ao novo produto
		E seleciona o botão "cancelar"
		E confirma o cancelamento
		Então o usuário retorna para a tela inicial do seu inventário
		E nenhuma informação é salva no sistema

	Cenário: Campos obrigatórios deixados em branco
		Quando o usuário insere as informações relacionadas ao novo produto
		Mas deixa campos obrigatórios em branco
		E clica no botão de adicionar
		Então o sistema informa o usuário que é necessário preencher todos os campos obrigatórios para fazer a adição
		E nenhuma informação é salva no sistema

	Cenário: Quantidade negativa
		Quando o usuário insere as informações relacionadas ao novo produto
		Mas insere quantidade negativa
		E clica no botão de adicionar
		Então o sistema informa o usuário que é necessário informar uma quantidade positiva para fazer a adição
		E nenhuma informação é salva no sistema

	Cenário: Erro interno do sistema
		Dado que ocorre um erro interno no sistema
		Quando o usuário insere as informações relacionadas ao novo produto
		E clica no botão de adicionar
		Então o usuário visualiza as informações que havia inserido
		E uma mensagem informando que não foi possível realizar a inserção
