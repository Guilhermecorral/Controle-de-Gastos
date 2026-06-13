import axios from 'axios'

type ApiErrorPayload = {
  message?: string
  details?: string[]
}

/**
 * Traduz erros da API para uma mensagem curta e util para a interface.
 */
export function getApiErrorMessage(error: unknown, fallbackMessage: string) {
  if (!axios.isAxiosError<ApiErrorPayload>(error)) {
    return fallbackMessage
  }

  if (!error.response) {
    return 'Não foi possível falar com o servidor agora. Verifique sua conexão e tente novamente em instantes.'
  }

  const responseData = error.response.data

  if (responseData?.details && responseData.details.length > 0) {
    return responseData.details[0]
      .replace(/^password:\s*/i, 'Senha: ')
      .replace(/^confirmPassword:\s*/i, 'Confirmacao da senha: ')
      .replace(/^email:\s*/i, 'E-mail: ')
      .replace(/^name:\s*/i, 'Nome: ')
  }

  if (responseData?.message) {
    return responseData.message
  }

  return fallbackMessage
}
