export const getUrlInfoWithHost = (fullUrl: string): RegExpMatchArray => {
    const reURLInformation = new RegExp(
        [
            '^(https?:)//', // protocol
            '(([^:/?#]*)(?::([0-9]+))?)', // host (hostname and port)
            '(/{0,1}[^?#]*)', // pathname
            '(\\?[^#]*|)', // search
            '(#.*|)$', // hash
        ].join(''),
    )

    return fullUrl.match(reURLInformation)
}
