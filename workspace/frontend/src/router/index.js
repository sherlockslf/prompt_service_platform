import { createRouter, createWebHistory } from 'vue-router'

const Login = () => import('../views/auth/Login.vue')
const AdminDashboard = () => import('../views/admin/Dashboard.vue')
const DeveloperDashboard = () => import('../views/developer/Dashboard.vue')
const ReleaseCenter = () => import('../views/developer/ReleaseCenter.vue')
const BusinessDashboard = () => import('../views/business/Dashboard.vue')
const PromptComposer = () => import('../views/business/PromptComposer.vue')
const CompositionReview = () => import('../views/developer/CompositionReview.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/admin',
    name: 'Admin',
    component: AdminDashboard
  },
  {
    path: '/developer',
    name: 'Developer',
    component: DeveloperDashboard
  },
  {
    path: '/developer/releases',
    name: 'ReleaseCenter',
    component: ReleaseCenter
  },
  {
    path: '/business',
    name: 'Business',
    component: BusinessDashboard
  },
  {
    path: '/business/composer',
    name: 'PromptComposerEntry',
    component: PromptComposer
  },
  {
    path: '/business/psus/:psuId/composer',
    name: 'PromptComposer',
    component: PromptComposer
  },
  {
    path: '/developer/psus/:psuId/reviews/:reviewId',
    name: 'CompositionReview',
    component: CompositionReview
  },
  {
    path: '/',
    name: 'Home',
    component: BusinessDashboard
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
