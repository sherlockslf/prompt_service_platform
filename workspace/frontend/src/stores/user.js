import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: {
      username: '',
      role: '',
      email: ''
    },
    isAuthenticated: false
  }),
  
  actions: {
    setUserInfo(userData) {
      this.userInfo = userData
      this.isAuthenticated = true
      if (userData.token) {
        this.token = userData.token
        localStorage.setItem('token', userData.token)
      }
    },
    
    logout() {
      this.token = ''
      this.userInfo = {
        username: '',
        role: '',
        email: ''
      }
      this.isAuthenticated = false
      localStorage.removeItem('token')
    },
    
    initializeFromStorage() {
      const token = localStorage.getItem('token')
      if (token) {
        this.token = token
        this.isAuthenticated = true
      }
    }
  }
})
